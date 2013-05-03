package de.stadtrallye.rallyesoft;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.comm.PushInit;

public class GCMIntentService extends GCMBaseIntentService {
	
	private static final String THIS = GCMIntentService.class.getSimpleName();

	private NotificationManager notes;

	public GCMIntentService()
	{
		super(PushInit.gcm);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		notes = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		
		NotificationCompat.BigTextStyle big = new NotificationCompat.BigTextStyle(
			new NotificationCompat.Builder(this)
				.setSmallIcon(android.R.drawable.stat_notify_chat)
				.setContentTitle("New GCM Message")
				.setContentText(extras.toString())
				.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0)))
			.bigText(extras.toString());
		
		notes.notify(":GCM Mesage", R.string.gcm_notification, big.build());
		
		Log.w("GCMIntentService.Push", "Note: " +extras);
		
		Log.w(THIS, extras.get("t").toString());
		
		if ("100".equals(extras.getString("t"))) { //TODO: discuss definition of int
			Model model = Model.getInstance(getApplicationContext(), true);
			IChatroom r = model.getChatroom(Integer.parseInt(extras.getString("d")));
			if (r != null) {
				r.refresh(); //TODO: specialize...
			} else
				Log.e(THIS, "Chat push received, but not my chatroom ("+extras.getInt("d")+")");
		}
	}

	@Override
	protected void onError(Context context, String errorId) {
		// TODO Auto-generated method stub
		Log.e("GCMIntentService", errorId);
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Log.i("GCMIntentService", "Registered GCM!");
		
		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Log.w("GCMIntentService", "Unregistered GCM!");
//		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
		
		Model.getInstance(getApplicationContext(), false).logout();
	}

}
