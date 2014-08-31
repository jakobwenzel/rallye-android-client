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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import de.stadtrallye.rallyesoft.common.Std;

/**
 * Created by Ramon on 02.10.13.
 */
public class ExpandableSplit {

	public ExpandableSplit(Bundle savedInstanceState) {

	}

	public static SharedPreferences getUiPreferences(Context context) {
		return context.getSharedPreferences(Std.CONFIG_UI, Context.MODE_PRIVATE);
	}
}
