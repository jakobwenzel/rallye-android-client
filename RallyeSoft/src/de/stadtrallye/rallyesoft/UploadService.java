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
import de.stadtrallye.rallyesoft.net.Request;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class UploadService extends IntentService {

	private static final String THIS = UploadService.class.getSimpleName();

	private static final String NOTE_TAG = ":uploader";

	private Model model;
	private NotificationManager notes;
	private String picture;

	public UploadService() {
		super("RallyePictureUpload");
		
		this.model = Model.getInstance(this);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		notes = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		picture = intent.getStringExtra(Std.PIC);
		String hash = intent.getStringExtra(Std.HASH);
		String mime = intent.getStringExtra(Std.MIME);

        long size = 0, current = 0;

        try {
			File f = new File(picture);
            size = f.length();
            FileInputStream fIn = new FileInputStream(f);

			URL url = model.getPictureUploadURL(hash);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(Request.RequestType.PUT.toString());
			con.setRequestProperty(Std.CONTENT_TYPE, mime);
			con.setDoOutput(true);

			NotificationCompat.Builder note = new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.upload)
					.setContentTitle(getString(R.string.uploading) + "...")
					.setContentText(picture);

			OutputStream out = con.getOutputStream();

			byte[] buffer = new byte[10000];
			int count = 1;
			while (count > 0) {
				count = fIn.read(buffer);
				out.write(buffer);
				current += count;

				notes.notify(NOTE_TAG, R.id.uploader, note.setProgress((int) size / 1000, (int) current / 1000, false).build());
			}

			notes.notify(NOTE_TAG, R.id.uploader, note.setProgress(0,0,false).setContentTitle(getString(R.string.upload_complete)).build());
			out.close();
			int code = con.getResponseCode();
			if (code >= 300) {
				Log.e(THIS, code + ": " + con.getResponseMessage());
			} else
				Log.i(THIS, picture +" successfully uploaded ("+size+" bytes)");
			con.disconnect();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
