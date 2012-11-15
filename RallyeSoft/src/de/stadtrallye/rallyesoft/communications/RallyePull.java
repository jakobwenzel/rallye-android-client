package de.stadtrallye.rallyesoft.communications;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;

import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.communications.Pull.Request;
import de.stadtrallye.rallyesoft.error.RestNotAvailableException;

public class RallyePull extends Pull {
	
	public RallyePull(String baseURL, String gcmID) {
		super(baseURL, gcmID);
	}
	
	
	public static RallyePull getPull() {
		return new RallyePull(Config.server, "");
	}
	
	public static RallyePull getPull(Bundle extras) {
		RallyePull pull = (RallyePull) extras.getSerializable("pull");
		if (pull == null)
			pull = RallyePull.getPull();
		return pull;
	}
	
	

	public JSONArray pullAllNodes() {
		Request r;
		try {
			r = new Request("/map/get/nodes");
		} catch (RestNotAvailableException e) {
			return null;
		}
		JSONArray res = r.getJSONArray();
		r.close();
		return res;
	}
	
	public JSONArray pullChats(int chatroom, int timestamp) {
		Request r;
		try {
			r = new Request("/chat/get");
			try {
				r.putPost(new JSONObject()
						.put(GCM, gcm)
						.put(CHATROOM, chatroom)
						.put(TIMESTAMP, (timestamp == 0)? JSONObject.NULL : timestamp)
						.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (RestNotAvailableException e) {
			return null;
		}
		JSONArray res = r.getJSONArray();
		r.close();
		return res;
	}
}
