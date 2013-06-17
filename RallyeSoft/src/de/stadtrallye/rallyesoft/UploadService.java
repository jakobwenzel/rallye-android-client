package de.stadtrallye.rallyesoft;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.Model;
import android.app.IntentService;
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
		String pic = intent.getStringExtra(Std.PIC);
		String hash = intent.getStringExtra(Std.HASH);
		
		URL url = model.getPictureUploadURL(hash);
		try {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("PUT");
			con.setDoOutput(true);
			
			con.getOutputStream();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//		NotificationCompat.BigTextStyle big = new NotificationCompat.BigTextStyle(
//			new NotificationCompat.Builder(this)
//				.setSmallIcon(android.R.drawable.stat_notify_chat)
//				.setContentTitle("New GCM Message")
//				.setContentText(extras.toString())
//				.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0)))
//			.bigText(extras.toString());
//		
//		notes.notify(":GCM Mesage", Std.GCM_NOTIFICATION, big.build());
	}


}
