package de.stadtrallye.rallyesoft.communications;

import android.content.Context;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

public class PushInit {
	
	final public static String gcm = "157370816729";

	public static void ensureRegistration(Context context) {
		// Register with GCM
        GCMRegistrar.checkDevice(context);
        GCMRegistrar.checkManifest(context);
        final String regId = GCMRegistrar.getRegistrationId(context);
        if (regId.equals("")) {
        	GCMRegistrar.register(context, gcm);
        	Log.v("gcm", "Now registered");
        } else {
        	Log.v("gcm", "Already registered: "+regId);
        }
        
	}
	
	public static String getID(Context context) {
		return GCMRegistrar.getRegistrationId(context);
	}

}
