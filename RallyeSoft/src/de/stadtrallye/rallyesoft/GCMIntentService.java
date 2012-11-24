package de.stadtrallye.rallyesoft;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
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
		Log.i("RPushService", "Received Push Notification! ");
	}

	@Override
	protected void onError(Context context, String errorId) {
		// TODO Auto-generated method stub
		Log.e("RPushService", errorId);
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Toast.makeText(getApplicationContext(), "GCM Push Registered!", Toast.LENGTH_SHORT).show();
		Log.i("RPushService", "Registered GCM!");
		
		RallyePull pull = RallyePull.getPull(getApplicationContext());
		pull.setGcmId(registrationId);
		
		try {
			pull.pushLogin(registrationId, Config.group, Config.password);
			
			GCMRegistrar.setRegisteredOnServer(getApplicationContext(), true);
		} catch (HttpResponseException e) {
			Log.e("RallyeGCM", "Unknown Http Exception:: " +e.toString());
		} catch (RestException e) {
			Log.e("RallyeGCM", "Unknown Rest Exception:: " +e.toString());
		} catch (JSONException e) {
			Log.e("RallyeGCM", "Unknown JSON Exception:: " +e.toString());
		}
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Toast.makeText(getApplicationContext(), "GCM Push Unregistered!", Toast.LENGTH_SHORT).show();
		Log.i("RPushService", "Unregistered GCM!");
		
		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
	}

}
