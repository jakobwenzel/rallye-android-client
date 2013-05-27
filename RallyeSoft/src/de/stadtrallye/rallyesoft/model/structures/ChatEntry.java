package de.stadtrallye.rallyesoft.model.structures;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.util.JSONArray;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class ChatEntry extends de.rallye.model.structures.ChatEntry {
	
	private static int me;
	private static int myGroup;

	public ChatEntry(int chatID, String message, long timestamp, int groupID, int userID, Integer pictureID) {
		super(chatID, message, timestamp, groupID, userID, pictureID);
	}
	
	public static void setMe(int userID, int groupID) {
		ChatEntry.me = userID;
		ChatEntry.myGroup = groupID;
	}
	
	public boolean isMe() {
		return me == userID;
	}
	
	public boolean isGroup() {
		return myGroup == groupID;
	}

	public static ArrayList<ChatEntry> translateJSON(String js) {
		return JSONArray.toList(new ChatConverter(), js);
	}
	
	public static class ChatConverter extends JSONConverter<ChatEntry> {
		
		@Override
		public ChatEntry doConvert(JSONObject o) throws JSONException {
				return new ChatEntry(o.getInt("chatID"),
							o.getString("message"),
							o.getInt("timestamp"),
							o.getInt("groupID"),
							o.getInt("userID"),
							o.getInt("pictureID"));
		}
	}
}
