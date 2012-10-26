package de.stadtrallye.rallyesoft;

import android.content.Context;
import android.content.Intent;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

	public GCMIntentService()
	{
		super("157370816729");
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onError(Context context, String errorId) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		// TODO Auto-generated method stub

	}

}
