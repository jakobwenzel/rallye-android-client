package de.stadtrallye.rallyesoft.uimodel;

/**
 * Created by Ramon on 12.08.13.
 */
public interface IPictureTakenListener {

	void pictureTaken(Picture picture);

	Picture getPicture();

	void sentPicture();

	public interface Picture {
		String getPath();
		String getHash();
	}
}
