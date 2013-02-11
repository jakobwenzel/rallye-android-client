package de.stadtrallye.rallyesoft.model;

import java.util.List;

public interface IPictureGallery extends List<Integer> {
	
//	public void setInitialPicture(int pictureID);
//	public List<Integer> getPictureList();
	public int getInitialPosition();
	
	public String getPictureUrl(int pos, char size);
}
