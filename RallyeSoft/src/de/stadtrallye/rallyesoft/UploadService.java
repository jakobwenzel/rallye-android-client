package de.stadtrallye.rallyesoft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.Model;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class UploadService extends IntentService {

	private Model model;
	private NotificationManager notes;
	private String uploading;
	private String picture;

	public UploadService() {
		super("RallyePictureUpload");
		
		this.model = Model.getInstance(this);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		notes = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		uploading = getString(R.string.uploading);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		picture = intent.getStringExtra(Std.PIC);
		String hash = intent.getStringExtra(Std.HASH);

        long size = 0, current = 0;

        try {
            size = new File(picture).length();
            FileInputStream fIn = getApplicationContext().openFileInput(picture);

			URL url = model.getPictureUploadURL(hash);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("PUT");
			con.setDoOutput(true);

			OutputStream out = con.getOutputStream();

			byte[] buffer = new byte[100];
			int count = 1;
			while (count > 0) {
				count = fIn.read(buffer);
				out.write(buffer);
				current += count;

				notify((int) size, (int) current);
			}
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void notify(int max, int current) {

		Notification note = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.upload)
				.setContentTitle(uploading + "...")
				.setContentText(uploading + ":  " + picture)
				.setProgress(max, current, false)
				.build();

		notes.notify(":uploader", R.id.uploader, note);
	}
}
