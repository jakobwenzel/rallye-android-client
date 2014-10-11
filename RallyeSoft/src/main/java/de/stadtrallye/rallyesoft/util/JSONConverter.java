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
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Special IConverter to convert already interpreted JSONObject's to T
 * @author Ramon
 *
 * @param <T> Target type
 */
public abstract class JSONConverter<T> implements IConverter<JSONObject, T> {
	
	@SuppressWarnings("SameReturnValue")
	public T fallback() {
		return null;
	}
	
	@Override
	public T convert(JSONObject input) {
		try {
			return doConvert(input);
		} catch (JSONException e) {
			Log.w("JSONConverter", "falling back, conversion failed", e);
			return fallback();
		}
	}
	
	public abstract T doConvert(JSONObject o) throws JSONException;
}
