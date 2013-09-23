package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import de.rallye.model.structures.GroupUser;
import de.stadtrallye.rallyesoft.util.JSONConverter;

/**
 * Common:ChatEntry enhanced by group name, user name and if one or both matches the currently logged in user
 * If a name is not available, the correspondent id is returned as String
 */
public class ChatEntry extends de.rallye.model.structures.ChatEntry {

	private String userName;
	private String groupName;

	public enum Sender { Me, MyGroup, SomeoneElse}
	
// --Commented out by Inspection START (22.09.13 02:44):
//	public ChatEntry(int chatID, String message, long timestamp, int groupID, String groupName, int userID, String userName, Integer pictureID) {
//		this(chatID, message, timestamp, groupID, userID, pictureID);
//
//		this.groupName = groupName;
//		this.userName = userName;
//	}
// --Commented out by Inspection STOP (22.09.13 02:44)

	public ChatEntry(int chatID, String message, long timestamp, int groupID, int userID, Integer pictureID) {
		super(chatID, message, timestamp, groupID, userID, (pictureID == 0)? null : pictureID);
	}

// --Commented out by Inspection START (22.09.13 02:44):
//	public void setUserName(String userName) {
//		this.userName = userName;
//	}
// --Commented out by Inspection STOP (22.09.13 02:44)

// --Commented out by Inspection START (22.09.13 02:44):
//	public void setGroupName(String groupName) {
//		this.groupName = groupName;
//	}
// --Commented out by Inspection STOP (22.09.13 02:44)

	public String getUserName() {
		return (userName != null)? userName : String.valueOf(userID);
	}

	public String getGroupName() {
		return (groupName != null)? groupName : String.valueOf(groupID);
	}
	
// --Commented out by Inspection START (22.09.13 02:44):
//	public Sender getSender(GroupUser user) {
//		return getSender(user, this.groupID, this.userID);
//	}
// --Commented out by Inspection STOP (22.09.13 02:44)

	public static Sender getSender(GroupUser user, int groupID, int userID) {
		if (userID == user.userID) {
			return Sender.Me;
		} else if (groupID == user.groupID) {
			return Sender.MyGroup;
		} else {
			return Sender.SomeoneElse;
		}
	}
}
