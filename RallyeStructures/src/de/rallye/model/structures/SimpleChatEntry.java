package de.rallye.model.structures;

public class SimpleChatEntry {
	
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
