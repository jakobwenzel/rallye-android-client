package de.stadtrallye.rallyesoft.uimodel;

/**
 * Mediator between the parts that 'take' / get the picture and the UI that shows the picture and eventually initiates sending it to the server
 * Scenarios: {@link de.stadtrallye.rallyesoft.MainActivity} receives the user selected Picture, submits it via {@link #pictureTaken(de.stadtrallye.rallyesoft.uimodel.IPictureTakenListener.Picture)} to ChatsFragment
 * 				each {@link de.stadtrallye.rallyesoft.fragments.ChatroomFragment} can access the selected picture via {@link #getPicture()} and dismiss the picture using {@link #sentPicture()}
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
