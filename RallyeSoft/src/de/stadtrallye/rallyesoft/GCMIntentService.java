package de.stadtrallye.rallyesoft;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

	public GCMIntentService()
	{
		super("157370816729");
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		Toast.makeText(getApplicationContext(), "Received Push Notification!", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onError(Context context, String errorId) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Toast.makeText(getApplicationContext(), "GCM Push Registered!", Toast.LENGTH_SHORT).show();
		
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Toast.makeText(getApplicationContext(), "GCM Push Unregistered!", Toast.LENGTH_SHORT).show();
	}

}
