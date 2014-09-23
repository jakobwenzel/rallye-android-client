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

package de.stadtrallye.rallyesoft.util;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Convert a existing JSONArray Entry by Entry using the supplied converter
 */
public class JSONArrayConverter<T> implements IConverter<JSONArray, List<T>> {

	private final JSONConverter<T> converter;

	public JSONArrayConverter(JSONConverter<T> converter) {
		this.converter = converter;
	}

	@Override
	public List<T> convert(JSONArray jr) {
		if (jr.length() > 0) {
			try {
				List<T> res = new ArrayList<T>();
				for (int i=0; i<jr.length(); i++) {
					res.add(converter.convert(jr.getJSONObject(i)));
				}
				return res;
			} catch (JSONException e) {
				return null;
			}
		} else
			return null;
	}
}
