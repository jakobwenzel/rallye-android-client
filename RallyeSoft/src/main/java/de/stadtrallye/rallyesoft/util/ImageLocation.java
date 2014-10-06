///*
// * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
// *
// * This file is part of RallyeSoft.
// *
// * RallyeSoft is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * Foobar is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
// */
//
//package de.stadtrallye.rallyesoft.util;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.Environment;
//import android.preference.PreferenceManager;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.widget.Toast;
//
//import java.io.File;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//import de.stadtrallye.rallyesoft.R;
//import de.stadtrallye.rallyesoft.services.UploadService;
//import de.stadtrallye.rallyesoft.common.Std;
//import de.stadtrallye.rallyesoft.model.pictures.IPictureManager.IPicture;
//
///**
// * Created by Jakob Wenzel on 05.10.13.
// */
//public class ImageLocation {
//
//	public static final String THIS = ImageLocation.class.getSimpleName();
//
//	/**
//	 * call in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)} to decode eventual information on a picture/media file as a result of an intent started by this class
//	 * @param requestCode received requestCode
//	 * @param resultCode received resultCode
//	 * @param data received Intent
//	 * @param context ApplicationContext to temporary save the Filename
//	 * @param autoUpload weather the selected Picture should be automatically uploaded using the Service {@link de.stadtrallye.rallyesoft.services.UploadService}
//	 * @return null if no picture was selected
//	 */
//	public static IPicture imageResult(int requestCode, int resultCode, Intent data, Context context, boolean autoUpload) {
//		if (resultCode== Activity.RESULT_OK) {
//			//Find uri
//			Uri uri=null;
//			int source = -1;
//			//It can either be returned with the intent parameter:
//			if (data != null) {
//				uri = data.getData();
//			} else {//else we use the saved value
//				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//				String uriString = prefs.getString(Std.CAMERA_OUTPUT_FILENAME,null);
//				source = prefs.getInt(Std.PICTURE_REQUEST_SOURCE, -1);
//				if (uriString!=null)
//					uri = Uri.parse(uriString);
//			}
//
//			if (uri != null) {
//				try {
//					//User has picked an image.
//
//					//Link to the image
//					final String imageFilePath = uri.toString();//cursor.getString(0);
//					final String hash = String.valueOf(imageFilePath.hashCode());
//
//					Log.i(THIS, "Picture taken/selected: " + imageFilePath);
//
//					Picture picture = new Picture(uri, hash, source);
//
//					if (autoUpload) {
//						Intent intent = new Intent(context, UploadService.class);
//						intent.putExtra(Std.PIC, imageFilePath);
//						intent.putExtra(Std.MIME, "image/jpeg");
//						intent.putExtra(Std.HASH, hash);
//						context.startService(intent);
//					}
//
//					return picture;
//				} catch (Exception e) {
//					Log.e(THIS, "Failed to select Picture", e);
//					Toast.makeText(context, R.string.picture_selection_failed, Toast.LENGTH_SHORT).show();
//					return null;
//				}
//			} else
//				return null;
//		} else
//			return null;
//	}
//}
//
////class Picture implements IPicture {
////
////	private final Uri path;
////	private final String hash;
////	private final int source;
////
////	private UploadState uploadState;
////
////	public Picture(Uri path, String hash, int source) {
////		this.path = path;
////		this.hash = hash;
////		this.source = source;
////
////		uploadState = UploadState.NotUploaded;
////	}
////
////
////
////	@Override
////	public Uri getPath() {
////		return path;
////	}
////
////	@Override
////	public String getHash() {
////		return hash;
////	}
////
////	@Override
////	public int getSource() {
////		return source;
////	}
////
////	@Override
////	public synchronized UploadState getUploadState() {
////		return null;
////	}
////
////	@Override
////	public synchronized void setStartUpload() {
////		uploadState = UploadState.Uploading;
////	}
////
////	@Override
////	public synchronized void setUploadComplete() {
////		uploadState = UploadState.UploadComplete;
////	}
////}