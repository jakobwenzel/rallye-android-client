package de.rallye.model.structures;

public class SimpleChatEntry {
	
	public static final String MESSAGE = "message";
	public static final String PICTURE_ID = "pictureID";
	
	public final String message;
	public final Integer pictureID;
	
	public SimpleChatEntry(String message, Integer pictureID) {
		this.message = message;
		this.pictureID = pictureID;
	}
	
	public SimpleChatEntry() {
		this.message = null;
		this.pictureID = null;
	}

	public boolean hasPicture() {
		return pictureID != null && pictureID > 0;
	}
	
	
}
