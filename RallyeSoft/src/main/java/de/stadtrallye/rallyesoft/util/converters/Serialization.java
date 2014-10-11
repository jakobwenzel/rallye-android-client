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

package de.stadtrallye.rallyesoft.util.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

/**
 * Created by Ramon on 22.09.2014.
 */
public class Serialization {
	private static ObjectMapper jsonMapper;
	private static ObjectMapper smileMapper;

	public static ObjectMapper getJsonInstance() {
		if (jsonMapper == null) {
			jsonMapper = new ObjectMapper();
		}
		return jsonMapper;
	}

	public static ObjectMapper getSmileInstance() {
		if (smileMapper == null) {
			smileMapper = new ObjectMapper(new SmileFactory());
		}
		return smileMapper;
	}
}
