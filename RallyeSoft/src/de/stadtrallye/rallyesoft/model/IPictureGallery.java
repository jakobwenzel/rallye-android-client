package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.rallye.model.structures.PictureSize;

public interface IPictureGallery extends List<Integer> {
	
	public int getInitialPosition();
	
	public PictureSize getImageSize();
	public void setImageSize(PictureSize size);
	
	public String getPictureUrl(int pos);
}
