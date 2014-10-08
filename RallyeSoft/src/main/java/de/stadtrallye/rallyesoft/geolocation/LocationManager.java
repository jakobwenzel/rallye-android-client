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

package de.stadtrallye.rallyesoft.geolocation;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;

import de.stadtrallye.rallyesoft.model.Server;
import retrofit.client.Response;

/**
 * Created by Ramon on 08.10.2014.
 */
public class LocationManager implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationSource, GoogleMap.OnMyLocationButtonClickListener, LocationListener {
	private final Mode mode;

	public enum Mode {ENERGY_SAVING, BURST, PING}

	private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 7846;
	private static final String THIS = LocationManager.class.getSimpleName();
	private final LocationClient locationClient;
	private final Context context;
	private OnLocationChangedListener mapsListener;

	public LocationManager(Context applicationContext) {
		this(applicationContext, Mode.ENERGY_SAVING);
	}

	public LocationManager(Context applicationContext, Mode mode) {
		this.context = applicationContext;
		locationClient = new LocationClient(applicationContext, this, this);
		this.mode = mode;

		if (mode == Mode.PING) {
			connect();
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.i(THIS, "Connected to Play Geo services");
//		Toast.makeText(context, "connected to Play Geo Services", Toast.LENGTH_SHORT).show();
		if (mode == Mode.PING) {
			startPingLocating();
		} else if (mode == Mode.ENERGY_SAVING) {
			startEnergySavingUpdates();
		} else if (mode == Mode.BURST) {
			startBurstLocating();
		}
	}

	private void startPingLocating() {
		LocationRequest request = new LocationRequest();
		request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		request.setNumUpdates(1);
		locationClient.requestLocationUpdates(request, this);
	}

	private void startEnergySavingUpdates() {
		LocationRequest request = new LocationRequest();
		request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
		request.setInterval(5000);
		request.setFastestInterval(500);
		request.setSmallestDisplacement(2);
		locationClient.requestLocationUpdates(request, this);
	}

	private void startBurstLocating() {
		LocationRequest request = new LocationRequest();
		request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		request.setFastestInterval(100);
		request.setInterval(500);
		request.setExpirationDuration(2000);
		locationClient.requestLocationUpdates(request, this);
	}

	private void stopAllUpdates() {
		locationClient.removeLocationUpdates(this);
	}

	@Override
	public void onDisconnected() {
		Log.i(THIS, "Disconnected from Play Geo Services");
//		Toast.makeText(context, "Disconnected to Play Geo Services", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(THIS, "Failed Play Geo Services: "+ connectionResult.getErrorCode());
//		if (connectionResult.hasResolution()) {
//			try {
//				// Start an Activity that tries to resolve the error
////				connectionResult.startResolutionForResult(this,	CONNECTION_FAILURE_RESOLUTION_REQUEST);
//                /*
//                * Thrown if Google Play services canceled the original
//                * PendingIntent
//                */
//			} catch (IntentSender.SendIntentException e) {
//				// Log the error
//				e.printStackTrace();
//			}
//		} else {
//            /*
//             * If no resolution is available, display a dialog to the
//             * user with the error.
//             */
//			showErrorDialog(connectionResult.getErrorCode());
//		}
		Toast.makeText(context, "Failed to connect to Play Geo Services, has resolution: "+ connectionResult.hasResolution(), Toast.LENGTH_SHORT).show();
	}

	public void connect() {
		locationClient.connect();
	}

	public void disconnect() {
//		locationClient.removeLocationUpdates(this);//TODO right now there are now updates to intents, when they are we cannot cancel here // is this neccessary?
		locationClient.disconnect();
	}

	@Override
	public void activate(OnLocationChangedListener onLocationChangedListener) {
		this.mapsListener = onLocationChangedListener;
	}

	@Override
	public void deactivate() {
		this.mapsListener = null;
	}

	@Override
	public boolean onMyLocationButtonClick() {
		startGoogleMapLocating();
		return false;// false means map will follow location
	}

	private void startGoogleMapLocating() {
		LocationRequest request = new LocationRequest();
		request.setExpirationDuration(10000);
		request.setFastestInterval(500);
		request.setInterval(1000);
		request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationClient.requestLocationUpdates(request, this);
	}

	@Override
	public void onLocationChanged(Location location) {
		if (mode == Mode.PING) {
			Response response = Server.getCurrentServer().getAuthCommunicator().sendCurrentLocation(location);

			Log.i(THIS, "Sent location to server: "+ response);

			disconnect();
			return;
		}

		if (mapsListener != null) {
			mapsListener.onLocationChanged(location);
		}
	}

	public Location getLastLocation() {
		return locationClient.getLastLocation();
	}
}
