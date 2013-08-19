package de.stadtrallye.rallyesoft.model.structures;

import de.rallye.model.structures.LatLng;

public class LatLngAdapter {

	public static com.google.android.gms.maps.model.LatLng toGms(LatLng alt) {
		return new com.google.android.gms.maps.model.LatLng(alt.latitude, alt.longitude);
	}
}
