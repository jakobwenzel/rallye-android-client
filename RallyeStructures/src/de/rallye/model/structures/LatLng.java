package de.rallye.model.structures;

public class LatLng {
	
	public static final String LAT = "latitude";
	public static final String LNG = "longitude";

	public final double latitude;
	public final double longitude;

	public LatLng(double lat, double lon) {
		this.latitude = lat;
		this.longitude = lon;
	}
}
