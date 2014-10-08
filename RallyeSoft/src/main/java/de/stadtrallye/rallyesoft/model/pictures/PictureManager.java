/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.model.pictures;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.services.UploadService;
import de.stadtrallye.rallyesoft.storage.IDbProvider;
import de.stadtrallye.rallyesoft.util.converters.Serialization;

import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Pictures;

/**
 * Wrapper to manage pictures requested and pictures waiting for upload
 *
 * Created by Ramon on 02.10.2014.
 */
public class PictureManager implements IPictureManager {

	private static final String THIS = PictureManager.class.getSimpleName();

	private static final int STATE_RESERVED = 1;
	private static final int STATE_SAVED = 4;
	private static final int STATE_CONFIRMED = 16;
	private static final int STATE_FAILED = 64;
	private static final int STATE_UPLOADING = 256;
	private static final int STATE_UPLOADED_PREVIEW = 512;
	private static final int STATE_UPLOADED = 1024;
	private static final int STATE_DISCARDED = 4096;

	private static String stateToString(int state) {
		switch (state) {
			case 1: return "Reserved";
			case 4: return "Saved";
			case 16: return "Confirmed";
			case 64: return "Failed";
			case 256: return "Uploading";
			case 512: return "Uploaded Preview";
			case 1024: return "Uploaded";
			case 4096: return "Discarded";
			default:
				return Integer.toString(state);
		}
	}

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	public static final int REQUEST_CODE = 519;

	public static final int SOURCE_CHAT = 1;
	public static final int SOURCE_SUBMISSION = 2;

	public static SourceHint getSourceHint(int source, String name) {
		return new SourceHint(source, name);
	}

	public static class SourceHint {
		public final String name;
		public final int source;

		@JsonCreator
		public SourceHint(@JsonProperty("source") int source, @JsonProperty("name") String name) {
			this.name = name;
			this.source = source;
		}
	}

	private final Context context;
	private final IDbProvider dbProvider;
	private final File mediaStorageDir;

	private boolean autoUpload = true;
	private final ConcurrentLinkedQueue<Picture> queue = new ConcurrentLinkedQueue<>();
	private Picture unconfirmed;
	private boolean queuedUnconfirmed = false;
	private final String deviceID;

	public PictureManager(Context applicationContext, IDbProvider dbProvider) {
		context = applicationContext;
		this.dbProvider = dbProvider;

		deviceID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), "rallye");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				Log.e(THIS, "failed to create directory");
				throw new UnsupportedOperationException("Failed to create directory");
			}
		}

		reload();
	}

	private void reload() {
		unconfirmed = null;
		queue.clear();

		Cursor c = getDb().query(Pictures.TABLE, new String[]{Pictures.KEY_ID, Pictures.KEY_STATE, Pictures.KEY_FILE, Pictures.KEY_SOURCE_HINT}, Pictures.KEY_STATE + "<=?", new String[]{Integer.toString(STATE_UPLOADED)}, null, null, null);
		while (c.moveToNext()) {
			SourceHint sourceHint = null;
			try {
				String s = c.getString(3);
				if (s != null)
					sourceHint = Serialization.getJsonInstance().readValue(s, SourceHint.class);
			} catch (IOException e) {
				Log.e(THIS, "Could not read SourceHint", e);
			}
			Picture picture = new Picture(c.getInt(0), c.getString(2), c.getInt(1), sourceHint);
			if (picture.isUnconfirmed()) {
				unconfirmed = picture;
				if (unconfirmed != null) {
					Log.w(THIS, "Multiple unconfirmed Pictures, discarding: "+ unconfirmed);
					unconfirmed.discard();
				}
				queuedUnconfirmed = autoUpload;
				if (autoUpload) {
					queue.add(picture);
				}
			} else {
				queue.add(picture);
			}
		}
		c.close();
		Log.d(THIS, "Loaded "+queue.size()+" Pictures into the queue");
		if (!queue.isEmpty())
			notifyUploader();
	}

	private Uri getPicturePlaceholderUri(int mediaType, SourceHint sourceHint){
		Uri uri = Uri.fromFile(getPicturePlaceholder(mediaType));

		saveReservedFilename(uri, sourceHint);
		return uri;
	}

	/**
	 * Generate a new Filename for the new picture
	 *
	 * persist this reserved Filename
	 * @return
	 */
	private File getPicturePlaceholder(int mediaType) {

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if (mediaType == MEDIA_TYPE_IMAGE){
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"IMG_"+ timeStamp + ".jpg");
		} else if(mediaType == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"VID_"+ timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}

	private void saveReservedFilename(Uri mediaFile, SourceHint sourceHint) {
		if (unconfirmed != null) {
			Log.w(THIS, "Overwriting unconfirmed Picture: "+ unconfirmed);
		}

		ContentValues insert = new ContentValues();
		insert.put(Pictures.KEY_FILE, mediaFile.toString());
		insert.put(Pictures.KEY_STATE, STATE_RESERVED);
		try {
			insert.put(Pictures.KEY_SOURCE_HINT, Serialization.getJsonInstance().writeValueAsString(sourceHint));
		} catch (JsonProcessingException e) {
			Log.e(THIS, "Could not serialize SourceHint", e);
		}

		int lastID = (int) getDb().insert(Pictures.TABLE, null, insert);
		Log.d(THIS, lastID+": reserved: "+ mediaFile.toString());
		unconfirmed = new Picture(lastID, mediaFile.toString(), STATE_RESERVED, sourceHint);
		queuedUnconfirmed = false;
	}

	/**
	 * Get an intent to either take a picture with the camera app or select an app that can pick an existing picture
	 */
	public Intent startPictureTakeOrSelect(SourceHint sourceHint) {
		//Attention: Our RequestCode will not be used for the result, if a jpeg is picked, data.getType will contain image/jpeg, if the picture was just taken with the camera it will be null
		Intent pickIntent = new Intent();
		pickIntent.setType("image/jpeg");
		pickIntent.setAction((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);

		Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		Uri fileUri = getPicturePlaceholderUri(PictureManager.MEDIA_TYPE_IMAGE, sourceHint); // reserve a filename to save the image
		takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
		takePhotoIntent.putExtra("return-data", true);

		Intent chooserIntent = Intent.createChooser(pickIntent, context.getString(R.string.select_take_picture));
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});

		return chooserIntent;
	}



	/**
	 * A picture has been picked
	 *
	 * (Meaning it is not available at the url previously reserved with getPicturePlaceholder)
	 * (Clears any reserved pictures)
	 * @param uri
	 */
	public Picture pickedPicture(Uri uri) {
		Picture picture = unconfirmed;
		if (picture == null) {
			Log.e(THIS, "No picture requested");
			return null;
		}

		picture.picked(uri);

		if (autoUpload) {
			queue.add(picture);
			queuedUnconfirmed = true;
			notifyUploader();
		}

		return picture;
	}

	/**
	 * A picture has been taken (it is available at the uri reserved with getPicturePlaceholder)
	 */
	public Picture tookPicture() {
		Picture picture = unconfirmed;
		if (picture == null) {
			Log.e(THIS, "No picture requested");
			return null;
		}

		picture.taken();

		if (autoUpload) {
			queue.add(picture);
			queuedUnconfirmed = true;
			notifyUploader();
		}

		return picture;
	}

	private void notifyUploader() {
		Log.d(THIS, "Requesting uploader to do some work");
		Intent intent = new Intent(context, UploadService.class);
		context.startService(intent);
	}

	/**
	 * Only has an effect on newly taken / picked pictures
	 * @param autoUpload whether or not pictures should start uploading as soon as they were chosen
	 */
	public void setAutoUpload(boolean autoUpload) {
		this.autoUpload = autoUpload;
	}

	private SQLiteDatabase getDb() {
		return dbProvider.getDatabase();
	}

	public ConcurrentLinkedQueue<Picture> getQueue() {
		return queue;
	}

	public boolean isPictureResult(int requestCode, int resultCode, Intent data) {
		return true;//TODO narrow checks!!
	}

	public Picture onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(THIS, "Received ActivityResult: Req: "+ requestCode +", res: "+ resultCode +", Intent: "+ data);
		if (resultCode== Activity.RESULT_OK) {
			if (unconfirmed == null) {
				Log.e(THIS, "No picture requested");
				throw new UnsupportedOperationException("uncomfirmed == null, should not have happened");
			}
			Uri uri=null;
			//It can either be returned with the intent parameter:
			if (data != null) {
				uri = data.getData();
				pickedPicture(uri);
			} else {//else we use the saved value
				tookPicture();
			}

			return unconfirmed;
		} else {
			Log.v(THIS, "Negative result code");
			return null;
		}
	}


	public class Picture implements IPicture {
		public final int pictureID;
		private String file;
		private int state;
		private final SourceHint sourceHint;
		private String hash;

		public Picture(int pictureID, String file, int state, SourceHint sourceHint) {
			this.pictureID = pictureID;
			this.file = file;
			this.state = state;
			this.sourceHint = sourceHint;
		}

		public SourceHint getSourceHint() {
			return sourceHint;
		}

		public boolean hasHash() {
			return hash != null;
		}

		public boolean isReserved() {
			return state == STATE_RESERVED;
		}

		public void calculateHash() {
			hash = calculateHash(file);
			Log.d(THIS, pictureID+": hash: "+ hash);
			ContentValues update = new ContentValues();
			update.put(Pictures.KEY_HASH, hash);
			updateDb(update);
		}

		/**
		 * Get a hash string of the image
		 * TODO: ideally hash the content
		 * @param fileName path to an image
		 * @return devID-fileName.hashCode()
		 */
		private String calculateHash(String fileName) {
			return deviceID+'-'+fileName.hashCode();
		}

		public void uploading() {
			ContentValues update = new ContentValues();
			setState(STATE_UPLOADING, update);
			updateDb(update);
		}

		public void uploaded() {
			ContentValues update = new ContentValues();
			setState(STATE_UPLOADED, update);
			updateDb(update);
			queue.remove(this);
		}

		public void uploadedPreview() {
			ContentValues update = new ContentValues();
			setState(STATE_UPLOADED_PREVIEW, update);
			updateDb(update);
		}

		private void setState(int state, ContentValues update) {
			Log.d(THIS, pictureID+": state: "+ stateToString(state));
			this.state = state;
			update.put(Pictures.KEY_STATE, state);
		}

		private void updateDb(ContentValues update) {
			getDb().update(Pictures.TABLE, update, Pictures.KEY_ID + "=?", new String[]{Integer.toString(pictureID)});
		}

		public void taken() {
			Log.d(THIS, pictureID+": taken: "+ file);
			ContentValues update = new ContentValues();
			setState(STATE_SAVED, update);
			updateDb(update);
		}

		public void picked(Uri uri) {
			Log.d(THIS, pictureID+": picked: "+ uri);
			file = uri.toString();
			ContentValues update = new ContentValues();
			setState(STATE_SAVED, update);
			update.put(Pictures.KEY_FILE, file);
			updateDb(update);
		}

		@Override
		public String getUri() {
			return file;
		}

		@Override
		public String getHash() {
			return hash;
		}

		public String getMimeType() {
			return "image/jpeg";
		}

		public void failed() {
			ContentValues update = new ContentValues();
			setState(STATE_FAILED, update);
			updateDb(update);
			// move to the end of the queue
			queue.remove(this);
			queue.add(this);
		}

		@Override
		public void discard() {
			ContentValues update = new ContentValues();
			setState(STATE_DISCARDED, update);
			updateDb(update);
			queue.remove(this);
			unconfirmed = null;
		}

		@Override
		public void confirm() {
			ContentValues update = new ContentValues();
			setState(STATE_CONFIRMED, update);
			updateDb(update);

			if (!queuedUnconfirmed) {
				queue.add(this);
				queuedUnconfirmed = true;
				notifyUploader();
			}

			unconfirmed = null;
		}

		public boolean isUnconfirmed() {
			return state < STATE_CONFIRMED;
		}

		public boolean isPreviewUploaded() {
			return state == STATE_UPLOADED_PREVIEW;
		}

		public boolean isUploading() {
			return state == STATE_UPLOADING;
		}
	}
}
