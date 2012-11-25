package de.stadtrallye.rallyesoft;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;

public class GCMIntentService extends GCMBaseIntentService {

	public GCMIntentService()
	{
		super(Config.gcm);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		Toast.makeText(getApplicationContext(), "Received Push Notification!", Toast.LENGTH_SHORT).show();
		Log.i("GCMIntentService", "Received Push Notification! ");
	}

	@Override
	protected void onError(Context context, String errorId) {
		// TODO Auto-generated method stub
		Log.e("RPushService", errorId);
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Log.i("GCMIntentService", "Registered GCM!");
		
		SharedPreferences pref = getSharedPreferences(getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
		if (pref.getString("server", null) == null) {
			Log.i("GCMIntentService", "Cannot Register on Server, no server configured");
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
		Toast.makeText(getApplicationContext(), "GCM Push Unregistered!", Toast.LENGTH_SHORT).show();
		Log.i("GCMIntentService", "Unregistered GCM!");
		
//		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
	}

}
