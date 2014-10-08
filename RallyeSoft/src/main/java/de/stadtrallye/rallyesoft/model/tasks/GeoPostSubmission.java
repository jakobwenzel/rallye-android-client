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

package de.stadtrallye.rallyesoft.model.tasks;

import android.location.Location;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.rallye.model.structures.PostSubmission;

/**
 * Created by Ramon on 08.10.2014.
 */
public class GeoPostSubmission extends PostSubmission {

	public final Location location;

	public GeoPostSubmission(@JsonProperty("submitType") int submitType, @JsonProperty("picSubmission") String picSubmission, @JsonProperty("intSubmission") Integer intSubmission, @JsonProperty("textSubmission") String textSubmission, @JsonProperty("location") Location location) {
		super(submitType, picSubmission, intSubmission, textSubmission);
		this.location = location;
	}
}
