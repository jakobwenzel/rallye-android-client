/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
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
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import de.stadtrallye.rallyesoft.services.UploadService;
import de.stadtrallye.rallyesoft.storage.Storage;

/**
 * Created by Ramon on 30.09.2014.
 */
public class NetworkStatusReceiver extends BroadcastReceiver {
	private static final String THIS = NetworkStatusReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Storage.aquireStorage(context, this);
		SharedPreferences pref = Storage.getAppPreferences();
		boolean pref_slowUpload = pref.getBoolean("slow_upload", false);
		boolean pref_meteredUpload = pref.getBoolean("metered_upload", true);
		boolean pref_previewUpload = pref.getBoolean("preview_upload", true);

		ConnectivityManager connection = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		boolean conn_available, conn_metered, conn_slow;

		NetworkInfo activeNetwork = connection.getActiveNetworkInfo();
		conn_available = activeNetwork.isConnected();

		switch (activeNetwork.getType()) {
			case (ConnectivityManager.TYPE_WIFI):
				conn_metered = false;
				conn_slow = false;
				break;
			case (ConnectivityManager.TYPE_MOBILE): {
				conn_metered = true;
				switch (telephony.getNetworkType()) {
					case (TelephonyManager.NETWORK_TYPE_LTE |
							TelephonyManager.NETWORK_TYPE_HSPAP | TelephonyManager.NETWORK_TYPE_HSPA)://TODO more + check
						conn_slow = false;
						break;
					case (TelephonyManager.NETWORK_TYPE_EDGE |
							TelephonyManager.NETWORK_TYPE_GPRS):
						conn_slow = true;
						break;
					default:
						conn_slow = false;
						break;
				}
				break;
			}
			default:
				conn_metered = false;
				conn_slow = false;
				break;
		}
		Log.d(THIS, "Network: available: " + conn_available + ", metered: " + conn_metered + ", slow: " + conn_slow);

		if(!(conn_slow && pref_slowUpload) && !(conn_metered && pref_meteredUpload) && conn_available) {
			Log.v(THIS, "starting uploader");
			context.startService(new Intent(context, UploadService.class));
		} else {
			Log.v(THIS, "Network not appropriate");
		}

		Storage.releaseStorage(this);
	}
}
