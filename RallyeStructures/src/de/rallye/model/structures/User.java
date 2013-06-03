package de.rallye.model.structures;

public class User {
	
	public static final String USER_ID = "userID";
	public static final String NAME = "name";
	
	final public int userID;
	final public String name;
	
	public User(int userID, String name) {
		this.userID = userID;
		this.name = name;
	}
}
