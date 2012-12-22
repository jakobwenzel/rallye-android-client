package de.stadtrallye.rallyesoft;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.Model;

public class GCMIntentService extends GCMBaseIntentService {

	private final SharedPreferences pref;
	private final NotificationManager notes;

	public GCMIntentService()
	{
		super(Config.gcm);
		
		notes = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		pref = getSharedPreferences(getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		
		final NotificationCompat.Builder b = new NotificationCompat.Builder(this);
		b
			.setSmallIcon(android.R.drawable.stat_notify_chat)
			.setContentTitle("New GCM Message")
			.setContentText(intent.toString())
			.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
		
		notes.notify(":GCM Mesage", R.string.gcm_notification, b.build());
		
		Log.w("GCMIntentService", "Received Push Notification:\n " +intent.toString());
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
		
		new Model(getApplicationContext(), pref).logout(null, 0);
	}

}
