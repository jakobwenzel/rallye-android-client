package de.rallye.model.structures;

public class UserLogin {

	final public int userID;
	final public int groupID;
	final public String password;
	
	public UserLogin(int userID, int groupID, String password) {
		this.userID = userID;
		this.groupID = groupID;
		this.password = password;
	}
	
	public String getHttpUser() {
		return userID +"@"+ groupID;
	}
}
