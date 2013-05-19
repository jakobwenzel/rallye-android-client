package de.stadtrallye.rallyesoft.model;

import de.rallye.model.structures.LatLng;

public interface IMap {
	void updateMap();
	
	
	void addListener(IMapListener l);
	void removeListener(IMapListener l);


	LatLng getMapLocation();


	void provideMap();


	float getZoomLevel();
}
