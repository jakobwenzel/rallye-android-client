package de.rallye.model.structures;

public class Chatroom {
	
	public static final String CHATROOM_ID = "chatroomID";
	public static final String NAME = "name";
	
	public final int chatroomID;
	public final String name;
	
	public Chatroom(int chatroomID, String name) {
		this.chatroomID = chatroomID;
		this.name = name;
	}
}
