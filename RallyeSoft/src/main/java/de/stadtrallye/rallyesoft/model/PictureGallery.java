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

package de.stadtrallye.rallyesoft.model;

import java.io.Serializable;

import de.rallye.model.structures.PictureSize;

public abstract class PictureGallery implements IPictureGallery, Serializable {

	protected PictureSize size = PictureSize.Standard;
	
	@Override
	public PictureSize getImageSize() {
		return size;
	}
	
	@Override
	public void setImageSize(PictureSize size) {
		this.size = size;
	}
}
