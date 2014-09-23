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

package de.stadtrallye.rallyesoft.model.executors;

import java.util.List;

import de.stadtrallye.rallyesoft.net.manual.Request;
import de.stadtrallye.rallyesoft.util.JSONConverter;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayConverter;

public class JSONArrayRequestExecutor<T, ID> extends RequestExecutor<List<T>, ID> {

	public JSONArrayRequestExecutor(Request req, JSONConverter<T> converter, Callback<ID> callback, ID callbackId) {
		super(req, new StringedJSONArrayConverter<T>(converter), callback, callbackId);
	}
}
