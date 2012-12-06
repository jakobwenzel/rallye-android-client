package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class JSONTranslator {
	
	public static ArrayList<ChatEntry> translateJSONChatEntrys(JSONArray js) {
		ArrayList<ChatEntry> messages = new ArrayList<ChatEntry>();
		
		try {
			JSONObject next;
			int i = 0;
			while ((next = (JSONObject) js.opt(i)) != null)
			{
				++i;
				messages.add(new ChatEntry(next.getString("message"), next.getInt("timestamp"), next.getInt("groupID"), 0));
			}
		} catch (Exception e) {
			Log.e("PullChat", e.toString());
			e.printStackTrace();
		}
		return messages;
	}
	
}
