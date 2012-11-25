package de.stadtrallye.rallyesoft.communications;

import android.content.Context;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.Config;

public class PushService {

	public static void ensureRegistration(Context context) {
		// Register with GCM
        GCMRegistrar.checkDevice(context);
        GCMRegistrar.checkManifest(context);
        final String regId = GCMRegistrar.getRegistrationId(context);
        if (regId.equals("")) {
        	GCMRegistrar.register(context, Config.gcm);
        	Log.v("gcm", "Now registered");
        } else {
        	Log.v("gcm", "Already registered: "+regId);
        }
        
	}
	
	public static String getID(Context context) {
		return GCMRegistrar.getRegistrationId(context);
	}

}
