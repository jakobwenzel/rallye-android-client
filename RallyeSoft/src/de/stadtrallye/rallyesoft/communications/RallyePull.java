package de.stadtrallye.rallyesoft.communications;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.communications.Pull.Request;
import de.stadtrallye.rallyesoft.error.RestNotAvailableException;

public class RallyePull extends Pull {
	
	public RallyePull(String baseURL, String gcmID) {
		super(baseURL, gcmID);
	}
	
	
	public static RallyePull getPull() {
		return new RallyePull(Config.server, "asdASDasdASD");
	}
	
	public static RallyePull getPull(Bundle extras) {
		Object tmp = (extras != null)? extras.getSerializable("pull") : null;
		
		return (tmp == null)? RallyePull.getPull() : (RallyePull) tmp;
	}
	
	
	
	public JSONArray pushLogin(String regId, int groupId, String pw) {
		try {
			Request r = makeRequest("/user/register");
			r.putPost(new JSONObject()
						.put(GCM, regId)
						.put(GROUP, groupId)
						.put(PASSWORD, pw)
						.toString(), Mime.JSON);
			
			JSONArray res = r.getJSONArray();
			return res;
		} catch(RestNotAvailableException e) {
			Log.e("RPull", e.toString());
			return null;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public JSONArray pullAllNodes() {
		Log.i("RPull", "pulling all nodes...");
		Request r;
		try {
			r = new Request("/map/get/nodes");
		} catch (RestNotAvailableException e) {
			Log.e("RPull", e.toString());
			return null;
		}
		JSONArray res = r.getJSONArray();
		r.close();
		return res;
	}
	
	public JSONArray pullChats(int chatroom, int timestamp) {
		Request r;
		try {
			Log.i("RPull", "pulling all chats...");
			r = new Request("/chat/get");
			try {
				r.putPost(new JSONObject()
						.put(GCM, gcm)
						.put(CHATROOM, chatroom)
						.put(TIMESTAMP, (timestamp == 0)? JSONObject.NULL : timestamp)
						.toString(), Pull.Mime.JSON);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (RestNotAvailableException e) {
			Log.w("RPull", e.toString());
			Log.w("RPull", e.getCause().toString());
			return null;
		}
		JSONArray res = r.getJSONArray();
		r.close();
		return res;
	}
}
