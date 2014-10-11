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

package de.stadtrallye.rallyesoft.model.pictures;

import android.util.Log;

import java.io.Serializable;

import de.rallye.model.structures.PictureSize;

public abstract class AbstractPictureGallery implements IPictureGallery, Serializable {

	private static final String THIS = AbstractPictureGallery.class.getSimpleName();
	protected PictureSize size = PictureSize.Standard;
	
	@Override
	public PictureSize getImageSize() {
		return size;
	}
	
	@Override
	public void setImageSize(PictureSize size) {
		if (size != null) {
			this.size = size;
		} else {
			size = PictureSize.Standard;
			Log.w(THIS, "Size is null, using Standard");
		}
	}
}
