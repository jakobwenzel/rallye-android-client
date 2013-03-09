package de.stadtrallye.rallyesoft.model.structures;

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
	public int chatID;

	public ChatEntry(int chatID, String message, int timestamp, int senderID, boolean self, int pictureID) {
		this.message = message;
		this.timestamp = timestamp;
		this.senderID = senderID;
		this.self = self;
		this.pictureID = pictureID;
		this.chatID = chatID;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChatEntry other = (ChatEntry) obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (pictureID != other.pictureID)
			return false;
		if (self != other.self)
			return false;
		if (senderID != other.senderID)
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}



	@Override
	public String toString() {
		return message +"|"+timestamp+"|"+senderID+"|"+self+"|"+pictureID;
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
