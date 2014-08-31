/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
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

package de.stadtrallye.rallyesoft.uimodel;

import android.os.Bundle;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.CameraPosition;

import de.rallye.model.structures.MapConfig;
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
		MapConfig config = model.getMap().getMapConfig();
		if (config != null) {
			de.rallye.model.structures.LatLng loc = config.location;
			gmo.camera(new CameraPosition(LatLngAdapter.toGms(loc), config.zoomLevel, 0, 0));
		}
		b.putParcelable(Std.GOOGLE_MAPS_OPTIONS, gmo);
		return  b;
	}
}
