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

package de.stadtrallye.rallyesoft.util;

import java.util.List;

/**
 * Converts String containing an JSON Array to a List<T> using another JSONConverter<T>
 * @author Ramon
 *
 * @param <T> Type of data in the separate fields of the JSON Array
 */
public class StringedJSONArrayConverter<T> implements IConverter<String, List<T>> {

	private final JSONConverter<T> converter;

	public StringedJSONArrayConverter(JSONConverter<T> converter) {
		this.converter = converter;
	}
	
	@Override
	public List<T> convert(String input) {
		return JSONArray.toList(converter, input);
	}
}
