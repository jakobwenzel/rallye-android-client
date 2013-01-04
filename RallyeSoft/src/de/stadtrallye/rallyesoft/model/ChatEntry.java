package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.util.JSONArray;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class ChatEntry {
	
//	private static ErrorHandling err = new ErrorHandling(ChatEntry.class.getSimpleName());
	
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
	
	public static ArrayList<ChatEntry> translateJSON(String js) {
		return JSONArray.toList(new ChatConverter(), js);
	}
	
	static class ChatConverter extends JSONConverter<ChatEntry> {
		
		@Override
		public ChatEntry doConvert(JSONObject o) throws JSONException {
				return new ChatEntry(o.getString("message"),
							o.getInt("timestamp"),
							o.getInt("groupID"),
							o.getBoolean("self"),
							(o.isNull("picture"))? 0 : o.getInt("picture"));
		}
	}
}
