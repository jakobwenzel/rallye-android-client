package de.rallye.model.structures;

public class UserAuth {
	
	public static final String USER_ID = "userID";
	public static final String PASSWORD = "password";

	final public int userID;
	final public String password;
	
	public UserAuth(int userID, String password) {
		this.userID = userID;
		this.password = password;
	}
	
	public String getHttpUser(int groupID) {
		return userID+"@"+groupID;
	}
}
