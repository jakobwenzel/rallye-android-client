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

package de.stadtrallye.rallyesoft.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.rallye.model.structures.Picture;
import de.rallye.model.structures.PictureSize;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.UploadOverviewActivity;
import de.stadtrallye.rallyesoft.model.IHandlerCallback;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.pictures.PictureManager;
import de.stadtrallye.rallyesoft.net.retrofit.RetroAuthCommunicator;
import de.stadtrallye.rallyesoft.receivers.NetworkStatusReceiver;
import de.stadtrallye.rallyesoft.storage.IDbProvider;
import de.stadtrallye.rallyesoft.storage.Storage;
import retrofit.RetrofitError;
import retrofit.mime.TypedOutput;

public class UploadService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String THIS = UploadService.class.getSimpleName();

	private static final String NOTE_TAG = ":uploader";

	private static final int MAX_SIZE = 1000;
	private static final int MSG_CHECK = 1;

	private NotificationManager notes;//TODO use AndroidNoticifationManager as wrapper ? useful?
	private ConnectivityManager connection;
	private SharedPreferences pref;
	private boolean pref_slowUpload;
	private boolean pref_meteredUpload;
	private boolean pref_previewUpload;
	private TelephonyManager telephony;
	private boolean conn_metered;
	private boolean conn_slow;
	private boolean conn_available;
	private IDbProvider dbProvider;
	private PictureManager pictureManager;
	private Looper looper;
	private UploaderHandler handler;
	private UploadStatus uploadStatus;
	private UploadBinder uploadBinder;

	public static class UploadStatus {
		public final PictureManager.Picture picture;
		public int biteCount;
		private int biteProgress;
		private boolean indeterminate;
		public final NotificationCompat.Builder notification;
		public long picSize;

		public UploadStatus(PictureManager.Picture picture, long picSize, int biteCount, NotificationCompat.Builder notification) {
			this.picture = picture;
			this.picSize = picSize;
			this.biteCount = biteCount;
			this.notification = notification;
		}

		public int getBiteProgress() {
			return biteProgress;
		}

		public boolean getIndeterminate() {
			return indeterminate;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		notes = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		connection = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		telephony = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		connection.getActiveNetworkInfo();

		Storage.aquireStorage(getApplicationContext(), this);
		dbProvider = Storage.getDatabaseProvider();
		pictureManager = Storage.getPictureManager();

		pref = Storage.getAppPreferences();
		pref.registerOnSharedPreferenceChangeListener(this);

		readPrefs();

		HandlerThread thread = new HandlerThread("UploadService [UploadThread]");
		thread.start();

		looper = thread.getLooper();
		handler = new UploaderHandler(looper);
	}

	private void readPrefs() {
		pref_slowUpload = pref.getBoolean("slow_upload", false);
		pref_meteredUpload = pref.getBoolean("metered_upload", true);
		pref_previewUpload = pref.getBoolean("preview_upload", true);
		Log.d(THIS, "Settings: preview: "+ pref_previewUpload +", metered: "+ pref_meteredUpload +", slow: "+ pref_slowUpload);
		pictureManager.setAutoUpload(pref.getBoolean("auto_upload", true));
	}

	private void updateNetworkStatus() {
		NetworkInfo activeNetwork = connection.getActiveNetworkInfo();
		conn_available = activeNetwork.isConnected();

		switch (activeNetwork.getType()) {
			case (ConnectivityManager.TYPE_WIFI):
				conn_metered = false;
				conn_slow = false;
				break;
			case (ConnectivityManager.TYPE_MOBILE): {
				conn_metered = true;
				switch (telephony.getNetworkType()) {
					case (TelephonyManager.NETWORK_TYPE_LTE |
							TelephonyManager.NETWORK_TYPE_HSPAP | TelephonyManager.NETWORK_TYPE_HSPA)://TODO more + check
						conn_slow = false;
						break;
					case (TelephonyManager.NETWORK_TYPE_EDGE |
							TelephonyManager.NETWORK_TYPE_GPRS):
						conn_slow = true;
						break;
					default:
						conn_slow = false;
						break;
				}
				break;
			}
			default:
				conn_metered = false;
				conn_slow = false;
				break;
		}
		Log.d(THIS, "Network: available: "+ conn_available +", metered: "+ conn_metered +", slow: "+ conn_slow);
	}

	private boolean isUploadAllowed() {
		return (!conn_slow || pref_slowUpload) && (!conn_metered || pref_meteredUpload) && conn_available;
	}

	private boolean isPreviewUpload() {
		return conn_slow || (conn_metered && pref_previewUpload);
	}

	private void setEnableNetworkStateListener(boolean enable) {
		ComponentName receiver = new ComponentName(getApplicationContext(), NetworkStatusReceiver.class);

		PackageManager pm = getApplicationContext().getPackageManager();

		pm.setComponentEnabledSetting(receiver,
				enable? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (uploadBinder == null) {
			uploadBinder = new UploadBinder();
		}
		return uploadBinder;
	}

	public class UploadBinder extends android.os.Binder {
		private IUploadListener listener;

		public Queue<PictureManager.Picture> getCurrentQueue() {
			return pictureManager.getQueue();
		}

		public void setListener(IUploadListener l) {
			this.listener = l;
		}

		private void notifyQueueChange() {
			if (listener != null) {
				listener.getCallbackHandler().post(new Runnable() {
					@Override
					public void run() {
						listener.onQueueChange();
					}
				});
			}
		}

		private void notifyUploadStatusChange() {
			if (listener != null)
				listener.getCallbackHandler().post(new Runnable() {
					@Override
					public void run() {
						listener.onUploadStatusChange();
					}
				});
		}

		public UploadStatus getUploadStatus() {
			return uploadStatus;
		}
	}

	public interface IUploadListener extends IHandlerCallback {

		void onUploadStatusChange();
		void onQueueChange();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Message msg = handler.obtainMessage();
		msg.arg1 = startId;
		handler.sendMessage(msg);
		
		return START_STICKY; // no need to redeliver, since the intent is only used as a nudge, we get our work from our own queue. Restart us if we were killed
	}

	private void upload(final PictureManager.Picture picture, boolean previewUpload) {
		final int biteSize = 8192 * 4;
		FileInputStream fileInputStream = null;

		picture.uploading();

		try {
			Uri uri = Uri.parse(picture.getUri());
			Log.d(THIS, "Source: "+picture.getUri());
			initReport(picture, biteSize);
			long fileSize = -1;
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					// Check for the freshest data.
					getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
				}
				AssetFileDescriptor fileDescriptor = getContentResolver().openAssetFileDescriptor(uri, "r");
				fileSize = fileDescriptor.getDeclaredLength();//TODO report as indeterminate progress if length unknown
				fileInputStream = fileDescriptor.createInputStream();
			} catch (SecurityException e) {
				Log.e(THIS, "No access rights... WTF", e);
				return;
			}

			TypedOutput uploadStream;
			RetroAuthCommunicator comm = Server.getCurrentServer().getAuthCommunicator();
			Picture responsePicture;

			final long picSize = fileSize;

			reportUploadBegins(picSize, biteSize);

			final FileInputStream fIn = fileInputStream;

			if (previewUpload) {
				Bitmap img = BitmapFactory.decodeStream(fileInputStream);

				int biggerSide = (img.getWidth() > img.getHeight()) ? img.getWidth() : img.getHeight();
				double factor = PictureSize.Preview.getDimension().height * 1.0 / biggerSide;

				int w = (int) Math.round(img.getWidth() * factor);
				int h = (int) Math.round(img.getHeight() * factor);
				final Bitmap scaled = Bitmap.createScaledBitmap(img, w, h, true);

				Log.i(THIS, "scaled bitmap. it now is " + w + "x" + h);

				uploadStream = new TypedOutput() {
					@Override
					public String fileName() {
						return picture.getHash();
					}

					@Override
					public String mimeType() {
						return "image/jpeg";
					}

					@Override
					public long length() {
						return -1;
					}

					@Override
					public void writeTo(OutputStream out) throws IOException {
						scaled.compress(Bitmap.CompressFormat.JPEG, 80, out);
					}
				};
				reportUploadIndeterminate(picture);
				responsePicture = comm.uploadPreviewPicture(picture.getHash(), uploadStream);
				picture.uploadedPreview();
			} else {
				uploadStream = new TypedOutput() {
					@Override
					public String fileName() {
						return picture.getHash();
					}

					@Override
					public String mimeType() {
						return picture.getMimeType();
					}

					@Override
					public long length() {
						return picSize;
					}

					@Override
					public void writeTo(OutputStream out) throws IOException {
						final byte[] buf = new byte[biteSize];
						int readSize = 0;
						int i = 0;

						while (readSize >= 0) {
							readSize = fIn.read(buf);
							out.write(buf);
							i++;
							reportUploadProgress(picture, i);
						}
					}
				};

				responsePicture = comm.uploadPicture(picture.getHash(), uploadStream);
				picture.uploaded();
			}

			if (responsePicture != null) {
				if (!responsePicture.pictureHash.equals(picture.getHash())) {
					//TODO picture.serverResponse(responsePicture), possibly rename file / hash, if the server would like to
					Log.w(THIS, "The server responded with a different hash than it was asked for... We should rename our file, but cannot since it is not yet implemented");
				}
			}

			reportUploadComplete(picture);

			Log.i(THIS, "Picture "+ picture.pictureID + " successfully uploaded (" + picSize + " bytes)");
			return;
		} catch (RetrofitError e) {
			if (e.isNetworkError()) {
				Log.e(THIS, "Retrofit Network error", e.getCause());
			} else {
				Log.e(THIS, "Server declined", e);
			}
		} catch (FileNotFoundException e) {
			Log.e(THIS, "File was not were it was supposed to be: "+ picture.getUri(), e);
			picture.discard();
			return;
		} catch (IOException e) {
			Log.e(THIS, "Upload failed", e);
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		picture.failed();
		reportUploadFailure(picture);
	}

	private void reportUploadFailure(PictureManager.Picture picture) {
		notes.notify(NOTE_TAG, R.id.uploader, uploadStatus.notification.setProgress(0, 0, false).setContentTitle(getString(R.string.upload_failed)).build());
	}

	private void initReport(PictureManager.Picture picture, int biteSize) {
		NotificationCompat.Builder note = new NotificationCompat.Builder(this)
				.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, UploadOverviewActivity.class), 0))
				.setSmallIcon(R.drawable.ic_upload_light)
				.setContentTitle(getString(R.string.uploading) + "...")
				.setContentText(getString(R.string.picture_no_x, picture.pictureID));

		uploadStatus = new UploadStatus(picture, -1, -1, note);
	}

	private void reportUploadBegins(long picSize, int biteSize) {

		uploadStatus.picSize = picSize;
		uploadStatus.biteCount = (int) (picSize / biteSize);
		uploadStatus.indeterminate = false;
		uploadStatus.biteProgress = 0;

		if (uploadBinder != null) {
			uploadBinder.notifyUploadStatusChange();
		}
	}

	private void reportUploadIndeterminate(PictureManager.Picture picture) {
		uploadStatus.indeterminate = true;
		notes.notify(NOTE_TAG, R.id.uploader, uploadStatus.notification.setProgress(0, 0, true).build());
		if (uploadBinder != null) {
			uploadBinder.notifyUploadStatusChange();
		}
	}

	private void reportUploadProgress(PictureManager.Picture picture, int i) {
		uploadStatus.biteProgress = i;
		uploadStatus.indeterminate = false;
		notes.notify(NOTE_TAG, R.id.uploader, uploadStatus.notification.setProgress(uploadStatus.biteCount, uploadStatus.biteProgress, false).build());
		if (uploadBinder != null) {
			uploadBinder.notifyUploadStatusChange();
		}
	}

	private void reportUploadComplete(PictureManager.Picture picture) {
		uploadStatus.biteProgress = uploadStatus.biteCount;
		uploadStatus.indeterminate = false;
		notes.notify(NOTE_TAG, R.id.uploader, uploadStatus.notification.setProgress(0, 0, false).setContentTitle(getString(R.string.upload_complete)).build());
		if (uploadBinder != null) {
			uploadBinder.notifyUploadStatusChange();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			looper.quitSafely();
		} else {
			looper.quit();
		}

		pref.unregisterOnSharedPreferenceChangeListener(this);
		Storage.releaseStorage(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		readPrefs();
	}

	private class UploaderHandler extends Handler {
		public UploaderHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			ConcurrentLinkedQueue<PictureManager.Picture> queue = pictureManager.getQueue();
			Log.d(THIS, "Uploader started, "+ queue.size() +" entries");

			boolean waitingOnWiFi = false;//TODO cache this information, avoid crawling the queue if we are only waiting for wifi but do not have it!!
			boolean waitingOnAnyNetwork = false;

			for (PictureManager.Picture picture : queue) {
				updateNetworkStatus();
				if (!isUploadAllowed()) {
					waitingOnAnyNetwork = true;
					break;
				}

				boolean previewUpload = isPreviewUpload();
				if (previewUpload && picture.isPreviewUploaded()) {
					waitingOnWiFi = true;
					continue;
				}

				if (!picture.hasHash())
					picture.calculateHash();

				upload(picture, previewUpload);
				if (uploadBinder != null) {
					uploadBinder.notifyQueueChange();
				}
			}

			if (!queue.isEmpty()) {
				Log.i(THIS, "Uploader paused, unsatisfactory network, waitOnWiFi: " + waitingOnWiFi + ", waitOnAny: " + waitingOnAnyNetwork);
				setEnableNetworkStateListener(true);
			} else {
				setEnableNetworkStateListener(false);
				Log.i(THIS, "Uploader finished");
			}

			boolean stopped = stopSelfResult(msg.arg1);
			if (stopped) {
				Log.d(THIS, "No more work, stopping Uploader Service");
			}
		}
	}
}
