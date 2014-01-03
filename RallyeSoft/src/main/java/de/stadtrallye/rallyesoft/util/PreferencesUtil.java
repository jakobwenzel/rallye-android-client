package de.stadtrallye.rallyesoft.util;

import android.content.Context;
import android.content.SharedPreferences;

import de.stadtrallye.rallyesoft.common.Std;

/**
 * Created by Ramon on 30.12.13.
 */
public abstract class PreferencesUtil{

	public static SharedPreferences getDefaultPreferences(Context context) {
		return context.getSharedPreferences(Std.CONFIG_MAIN, Context.MODE_PRIVATE);
	}

	public static SharedPreferences getGcmPreferences(Context context) {
		return context.getSharedPreferences(Std.CONFIG_GCM, Context.MODE_PRIVATE);
	}

}
