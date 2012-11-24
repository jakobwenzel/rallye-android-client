package de.stadtrallye.rallyesoft.communications;

import android.app.Activity;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.Config;

public class PushService {

	public static void ensureRegistration(Activity activity) {
		// Register with GCM
        GCMRegistrar.checkDevice(activity);
        GCMRegistrar.checkManifest(activity);
        final String regId = GCMRegistrar.getRegistrationId(activity);
        if (regId.equals("")) {
        	GCMRegistrar.register(activity, Config.gcm);
        	Log.v("gcm", "Now registered");
        } else {
        	Log.v("gcm", "Already registered");
        }
        
	}
	
	public static String getID(Activity activity) {
		return GCMRegistrar.getRegistrationId(activity);
	}

}
