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

package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.CameraPosition;

import de.rallye.model.structures.MapConfig;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.map.IMapManager;
import de.stadtrallye.rallyesoft.model.structures.LatLngAdapter;

/**
 * Static Methods that do not belong anywhere else?
 */
public class Util {

	public static Bundle getDefaultMapOptions(IMapManager mapManager) {
		Bundle b = new Bundle();
		GoogleMapOptions gmo = new GoogleMapOptions().compassEnabled(true);

		boolean lateInit = false;

		if (mapManager != null) {
			MapConfig mapConfig = mapManager.getMapConfigCached();
			if (mapConfig != null) {
				de.rallye.model.structures.LatLng loc = mapConfig.location;
				gmo.camera(new CameraPosition(LatLngAdapter.toGms(loc), mapConfig.zoomLevel, 0, 0));
			} else {
				lateInit = true;
				mapManager.updateMapConfig();
			}

		} else {
			lateInit = true;
		}

		b.putBoolean(Std.LATE_INITIALIZATION, lateInit);
		b.putParcelable(Std.GOOGLE_MAPS_OPTIONS, gmo);
		return  b;
	}

	public static CharSequence resolveDayOfWeek(Context context, int dayOfWeek) {
		return context.getResources().getTextArray(R.array.daysOfWeek)[dayOfWeek%7];
	}

	public static boolean isHttpPasswordIncorrect(int status) {
		return status == 401;
	}
}
