package de.stadtrallye.rallyesoft.model;

import com.google.android.gms.maps.model.LatLng;

public interface IMap {
	void updateMap();
	
	
	void addListener(IMapListener l);
	void removeListener(IMapListener l);


	LatLng getMapLocation();


	void provideMap();


	float getZoomLevel();
}
