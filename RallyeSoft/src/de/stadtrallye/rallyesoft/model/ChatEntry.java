package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class ChatEntry {
	
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
	
	public static ArrayList<ChatEntry> translateJSON(JSONArray js) {
		ArrayList<ChatEntry> messages = new ArrayList<ChatEntry>();
				
		try {
			JSONObject next;
			int i = 0;
			while ((next = (JSONObject) js.opt(i)) != null)
			{
				++i;
				messages.add(
					new ChatEntry(next.getString("message"),
						next.getInt("timestamp"),
						next.getInt("groupID"),
						next.getBoolean("self"),
						(next.isNull("picture"))? 0 : next.getInt("picture")));
			}
		} catch (Exception e) {
			Log.e("PullChat", e.toString());
			e.printStackTrace();
		}
		return messages;
	}
}
