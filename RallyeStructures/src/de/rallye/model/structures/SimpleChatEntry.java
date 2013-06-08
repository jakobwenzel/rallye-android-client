package de.rallye.model.structures;

public class SimpleChatEntry {
	
	public static final String MESSAGE = "message";
	public static final String PICTURE_ID = "pictureID";
	
	public String message;
	public Integer pictureID;

	public boolean hasPicture() {
		return pictureID != null && pictureID > 0;
	}
	
	
}
