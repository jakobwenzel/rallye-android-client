package de.stadtrallye.rallyesoft.model;

public class ChatEntry {
	
	public String message;
	public int timestamp;
	public int senderID;
	public int pictureID;
	public boolean self;

	public ChatEntry(String message, int timestamp, int senderID, boolean self, int pictureID) {
		this.message = message;
		this.timestamp = timestamp;
		this.senderID = senderID;
		this.self = self;
		this.pictureID = pictureID;
	}
}
