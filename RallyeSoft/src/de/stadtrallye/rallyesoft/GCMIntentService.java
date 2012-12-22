package de.stadtrallye.rallyesoft;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.communications.PushInit;
import de.stadtrallye.rallyesoft.model.Model;

public class GCMIntentService extends GCMBaseIntentService {

	private SharedPreferences pref;
	private NotificationManager notes;

	public GCMIntentService()
	{
		super(PushInit.gcm);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		notes = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		pref = getSharedPreferences(getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		String extras = intent.getExtras().toString();
		
		NotificationCompat.BigTextStyle big = new NotificationCompat.BigTextStyle(
			new NotificationCompat.Builder(this)
				.setSmallIcon(android.R.drawable.stat_notify_chat)
				.setContentTitle("New GCM Message")
				.setContentText(extras)
				.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0)))
			.bigText(extras);
		
		notes.notify(":GCM Mesage", R.string.gcm_notification, big.build());
		
		Log.w("GCMIntentService", "Received Push Notification:\n " +extras);
	}

	@Override
	protected void onError(Context context, String errorId) {
		// TODO Auto-generated method stub
		Log.e("GCMIntentService", errorId);
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Log.i("GCMIntentService", "Registered GCM!");
		
		
		if (pref.getString("server", null) == null) {
			Log.w("GCMIntentService", "Cannot Register on Server, no server configured");
			return;
		}
		
		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
		
//		RallyePull pull = RallyePull.getPull(getApplicationContext());
//		pull.setGcmId(registrationId);
//		
//		try {
//			JSONArray res = pull.pushLogin();
//			
//			GCMRegistrar.setRegisteredOnServer(getApplicationContext(), true);
//			Log.i("RPushService", "Registered on Server");
//		} catch (HttpResponseException e) {
//			Log.e("RallyeGCM", "Unknown Http Exception:: " +e.toString());
//		} catch (RestException e) {
//			Log.e("RallyeGCM", "Unknown Rest Exception:: " +e.toString());
//		} catch (JSONException e) {
//			Log.e("RallyeGCM", "Unknown JSON Exception:: " +e.toString());
//		}
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Log.w("GCMIntentService", "Unregistered GCM!");
//		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
		
		Model.getInstance(getApplicationContext(), pref, false).logout(null, 0);
	}

}
