package de.stadtrallye.rallyesoft.model.structures;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.util.JSONArray;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class ChatEntry extends de.rallye.model.structures.ChatEntry{
	
	public ChatEntry(int chatID, String message, int timestamp, int senderID, boolean self, int pictureID) {
		super(chatID, message, timestamp, senderID, self, pictureID);
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
							o.getBoolean("self"),
							(o.isNull("picture"))? 0 : o.getInt("picture"));
		}
	}
}
