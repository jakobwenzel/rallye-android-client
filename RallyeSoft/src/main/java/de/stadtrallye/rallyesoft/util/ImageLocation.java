/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.services.UploadService;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.uimodel.IPicture;

/**
 * Created by Jakob Wenzel on 05.10.13.
 */
public class ImageLocation {

	public static final String THIS = ImageLocation.class.getSimpleName();

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;



    /** Create a file Uri for saving an image or video */
    public static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    public static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "rallye");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

	/**
	 * Start a intent to either take a picture with the camera app or select a app that can pick an existing picture
	 * @param activity Activity that will receive the Intent result and should process by calling {@link #imageResult(int, int, android.content.Intent, android.content.Context, boolean)}
	 */
	public static void startPictureTakeOrSelect(Activity activity, int source) {
		//Attention: Our RequestCode will not be used for the result, if a jpeg is picked, data.getType will contain image/jpeg, if the picture was just taken with the camera it will be null
		Intent pickIntent = new Intent();
		pickIntent.setType("image/jpeg");
		pickIntent.setAction(Intent.ACTION_GET_CONTENT);

		Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		Uri fileUri = ImageLocation.getOutputMediaFileUri(ImageLocation.MEDIA_TYPE_IMAGE); // create a file to save the image
		takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).edit();
		editor.putString(Std.CAMERA_OUTPUT_FILENAME,fileUri.toString());
		editor.putInt(Std.PICTURE_REQUEST_SOURCE, source);
		editor.apply();

		takePhotoIntent.putExtra("return-data", true);

		Intent chooserIntent = Intent.createChooser(pickIntent, activity.getString(R.string.select_take_picture));//TODO: !!! attach chatroom to picturerequest
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});

		activity.startActivityForResult(chooserIntent, Std.PICK_IMAGE);
	}

	/**
	 * call in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)} to decode eventual information on a picture/media file as a result of an intent started by this class
	 * @param requestCode received requestCode
	 * @param resultCode received resultCode
	 * @param data received Intent
	 * @param context ApplicationContext to temporary save the Filename
	 * @param autoUpload weather the selected Picture should be automatically uploaded using the Service {@link de.stadtrallye.rallyesoft.services.UploadService}
	 * @return null if no picture was selected
	 */
	public static IPicture imageResult(int requestCode, int resultCode, Intent data, Context context, boolean autoUpload) {
		if (resultCode== Activity.RESULT_OK) {
			//Find uri
			Uri uri=null;
			int source = -1;
			//It can either be returned with the intent parameter:
			if (data != null) {
				uri = data.getData();
			} else {//else we use the saved value
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				String uriString = prefs.getString(Std.CAMERA_OUTPUT_FILENAME,null);
				source = prefs.getInt(Std.PICTURE_REQUEST_SOURCE, -1);
				if (uriString!=null)
					uri = Uri.parse(uriString);
			}

			if (uri != null) {
				try {
					//User has picked an image.

					//Link to the image
					final String imageFilePath = uri.toString();//cursor.getString(0);
					final String hash = String.valueOf(imageFilePath.hashCode());

					Log.i(THIS, "Picture taken/selected: " + imageFilePath);

					Picture picture = new Picture(uri, hash, source);

					if (autoUpload) {
						Intent intent = new Intent(context, UploadService.class);
						intent.putExtra(Std.PIC, imageFilePath);
						intent.putExtra(Std.MIME, "image/jpeg");
						intent.putExtra(Std.HASH, hash);
						context.startService(intent);
					}

					return picture;
				} catch (Exception e) {
					Log.e(THIS, "Failed to select Picture", e);
					Toast.makeText(context, R.string.picture_selection_failed, Toast.LENGTH_SHORT).show();
					return null;
				}
			} else
				return null;
		} else
			return null;
	}
}

class Picture implements IPicture {

	private final Uri path;
	private final String hash;
	private final int source;

	private UploadState uploadState;

	public Picture(Uri path, String hash, int source) {
		this.path = path;
		this.hash = hash;
		this.source = source;

		uploadState = UploadState.NotUploaded;
	}



	@Override
	public Uri getPath() {
		return path;
	}

	@Override
	public String getHash() {
		return hash;
	}

	@Override
	public int getSource() {
		return source;
	}

	@Override
	public synchronized UploadState getUploadState() {
		return null;
	}

	@Override
	public synchronized void setStartUpload() {
		uploadState = UploadState.Uploading;
	}

	@Override
	public synchronized void setUploadComplete() {
		uploadState = UploadState.UploadComplete;
	}
}