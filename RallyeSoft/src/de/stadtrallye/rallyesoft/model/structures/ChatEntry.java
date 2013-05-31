package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class ChatEntry extends de.rallye.model.structures.ChatEntry {

	public enum Sender { Me, MyGroup, SomeoneElse};
	
	public ChatEntry(int chatID, String message, long timestamp, int groupID, int userID, Integer pictureID) {
		super(chatID, message, timestamp, groupID, userID, pictureID);
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
