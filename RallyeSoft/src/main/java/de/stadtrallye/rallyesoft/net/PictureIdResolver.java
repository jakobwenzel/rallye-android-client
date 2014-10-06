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

package de.stadtrallye.rallyesoft.net;

import java.io.Serializable;

import de.rallye.model.structures.PictureSize;

/**
 * Serializable Class to generate Picture URLs (as String) from a pictureHash and PictureSize
 * So PictureGallery's do not have to hold on to Model, which is not serializable
 */
public class PictureIdResolver implements Serializable {

	private final String base;

	public PictureIdResolver(String base) {
		this.base = base;
	}

	public String resolvePictureID(String hash, PictureSize size) {
		return base + Paths.getPic(hash, size);
	}
}
