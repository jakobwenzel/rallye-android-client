package de.stadtrallye.rallyesoft.communications;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

public class PushService {

	public PushService(Activity activity) {
		// Register with GCM
        GCMRegistrar.checkDevice(activity);
        GCMRegistrar.checkManifest(activity);
        final String regId = GCMRegistrar.getRegistrationId(activity);
        if (regId.equals("")) {
        	GCMRegistrar.register(activity, "157370816729");
        	Log.v("gcm", "Now registered");
        } else {
        	Log.v("gcm", "Already registered");
        }
        Toast.makeText(activity.getApplicationContext(), "Requested Push Registration!", Toast.LENGTH_SHORT).show();
	}

}
