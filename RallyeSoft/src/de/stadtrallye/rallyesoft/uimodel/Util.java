package de.stadtrallye.rallyesoft.uimodel;

import android.os.Bundle;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.CameraPosition;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.structures.LatLngAdapter;

/**
 * Static Methods that do not belong anywhere else?
 */
public class Util {

	public static Bundle getDefaultMapOptions(IModel model) {
		Bundle b = new Bundle();
		GoogleMapOptions gmo = new GoogleMapOptions().compassEnabled(true);
		de.rallye.model.structures.LatLng loc = model.getMap().getMapLocation();
		if (loc != null) {
			gmo.camera(new CameraPosition(LatLngAdapter.toGms(loc), model.getMap().getZoomLevel(), 0, 0));
		}
		b.putParcelable(Std.GOOGLE_MAPS_OPTIONS, gmo);
		return  b;
	}
}
