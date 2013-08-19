package de.stadtrallye.rallyesoft.model;

import java.io.Serializable;
import java.util.List;

import de.rallye.model.structures.PictureSize;

public interface IPictureGallery extends Serializable {
	
	int getInitialPosition();

	int getCount();
	
	PictureSize getImageSize();
	void setImageSize(PictureSize size);
	
	String getPictureUrl(int pos);
}
