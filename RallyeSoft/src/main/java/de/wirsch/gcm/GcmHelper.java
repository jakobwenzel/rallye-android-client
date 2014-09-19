/*
 * Copyright (c) 2014 Ramon Wirsch.
 *
 * This file is part of RallySoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.wirsch.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.util.PreferencesUtil;

/**
 * Created by Ramon on 30.12.13.
 */
public abstract class GcmHelper {

	final private static String SENDER_ID = "157370816729";

	private static final String THIS = GcmHelper.class.getSimpleName();

	public interface IGcmListener {
		void onDelayedGcmId(String gcmId);
	}

	private static IGcmListener listener;
	private static String gcmId;

	public static boolean ensureRegistration(Context context) {
		gcmId = readGcmId(context);

		if (gcmId == null) {
			registerInBackground(context);
		}

		return gcmId != null;
	}

	public static String getGcmId() {
		return gcmId;
	}

	public static void setGcmListener(IGcmListener listener) {
		GcmHelper.listener = listener;
		if (gcmId != null) {
			listener.onDelayedGcmId(gcmId);
		}
	}

	private static String readGcmId(Context context) {
		final SharedPreferences prefs = PreferencesUtil.getGcmPreferences(context);
		gcmId = prefs.getString(Std.GCM_ID, null);
		if (gcmId == null) {
			Log.i(THIS, "Registration not found.");
			return null;
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(Std.APP_VERSION, 0);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(THIS, "App version changed.");
			return null;
		}
		return gcmId;
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	public static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	private static void registerInBackground(final Context context) {
		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				try {
					GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

					gcmId = gcm.register(SENDER_ID);
					Log.i(THIS, "Device registered, registration ID=" + gcmId);

					// You should send the registration ID to your server over HTTP,
					// so it can use GCM/HTTP or CCS to send messages to your app.
					// The request to your server should be authenticated if your app
					// is using accounts.
					if (listener != null)
						listener.onDelayedGcmId(gcmId);

					// For this demo: we don't need to send it because the device
					// will send upstream messages to a server that echo back the
					// message using the 'from' address in the message.

					// Persist the regID - no need to register again.
					storeRegistrationId(context);
					return gcmId;
				} catch (IOException ex) {
					Log.e(THIS, "Error getting registrationId", ex);
					return null;
				}
			}
		}.execute();

	}

	private static void storeRegistrationId(Context context) {
		final SharedPreferences prefs = PreferencesUtil.getGcmPreferences(context);
		int appVersion = getAppVersion(context);
		Log.i(THIS, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(Std.GCM_ID, gcmId);
		editor.putInt(Std.APP_VERSION, appVersion);
		editor.apply();
	}
}
