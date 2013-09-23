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
