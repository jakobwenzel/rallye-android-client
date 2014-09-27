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

package de.stadtrallye.rallyesoft.util.converters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Helpers to enable saving and reading Set<String> to and from {@link android.content.SharedPreferences}, although the actual method is not available before API 11
 */
public class PreferenceConverters {

	public static Set<String> fromSingleString(String s) {
		HashSet<String> res = new HashSet<String>();

		if (s.length() > 0) {
			String[] str = s.split("\\|");
			Collections.addAll(res, str);
		}
		return res;
	}

	public static String toSingleString(Set<String> set) {
		StringBuilder sb = new StringBuilder();

		for (String s :set) {
			sb.append(s).append('|');
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}


}
