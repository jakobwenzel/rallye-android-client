package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import de.rallye.model.structures.GroupUser;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class ChatEntry extends de.rallye.model.structures.ChatEntry {

	private String userName;
	private String groupName;

	public enum Sender { Me, MyGroup, SomeoneElse}
	
	public ChatEntry(int chatID, String message, long timestamp, int groupID, String groupName, int userID, String userName, Integer pictureID) {
		this(chatID, message, timestamp, groupID, userID, pictureID);

		this.groupName = groupName;
		this.userName = userName;
	}

	public ChatEntry(int chatID, String message, long timestamp, int groupID, int userID, Integer pictureID) {
		super(chatID, message, timestamp, groupID, userID, (pictureID == 0)? null : pictureID);
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getUserName() {
		return (userName != null)? userName : String.valueOf(userID);
	}

	public String getGroupName() {
		return (groupName != null)? groupName : String.valueOf(groupID);
	}
	
	public Sender getSender(GroupUser user) {
		if (userID == user.userID) {
			return Sender.Me;
		} else if (groupID == user.groupID) {
			return Sender.MyGroup;
		} else {
			return Sender.SomeoneElse;
		}
	}
	
	public static class ChatConverter extends JSONConverter<ChatEntry> {
		
		@Override
		public ChatEntry doConvert(JSONObject o) throws JSONException {
				return new ChatEntry(o.getInt(ChatEntry.CHAT_ID),
							o.getString(ChatEntry.MESSAGE),
							o.getInt(ChatEntry.TIMESTAMP),
							o.getInt(ChatEntry.GROUP_ID),
							o.getInt(ChatEntry.USER_ID),
							o.getInt(ChatEntry.PICTURE_ID));
		}
	}
}
