package de.stadtrallye.rallyesoft;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.communications.RallyePull;

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
		
		RallyePull pull = RallyePull.getPull();
		
		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), true);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Toast.makeText(getApplicationContext(), "GCM Push Unregistered!", Toast.LENGTH_SHORT).show();
		Log.i("RPushService", "Unregistered GCM!");
	}

}
