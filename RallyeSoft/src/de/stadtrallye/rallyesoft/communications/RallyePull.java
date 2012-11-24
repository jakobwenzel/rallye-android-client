package de.stadtrallye.rallyesoft.communications;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;

public class RallyePull extends Pull {
	
	private String gcm = "";
	private Context context;
	
	private final static String GCM = "gcm";
	private final static String TIMESTAMP = "timestamp";
	private final static String CHATROOM = "chatroom";
	private final static String PASSWORD = "password";
	private final static String GROUP = "groupID";
	
	public RallyePull(String baseURL, Context context) {
		super(baseURL);
		this.context = context;
	}
	
	
	public static RallyePull getPull(Context context) {
		return new RallyePull(Config.server, context);
	}
	
	/*public static RallyePull getPull(Bundle extras) {
		Object tmp = (extras != null)? extras.getSerializable("pull") : null;
		
		return (tmp == null)? RallyePull.getPull() : (RallyePull) tmp;
	}*/
	
	
	
	public JSONArray pushLogin(String regId, int groupId, String pw) throws HttpResponseException, RestException, JSONException {
		Request r = makeRequest("/user/register");
		try {
			r.putPost(new JSONObject()
						.put(GCM, regId)
						.put(GROUP, groupId)
						.put(PASSWORD, pw)
						.toString(), Mime.JSON);
		} catch (JSONException e) {
			Log.e("PullChat", "Unkown JSON error during POST");
		}
		JSONArray res = r.getJSONArray();
		return res;
	}

	public JSONArray pullAllNodes() throws HttpResponseException, RestException, JSONException {
		Log.i("RPull", "pulling all nodes...");
		Request r;
		r = new Request("/map/get/nodes");
		JSONArray res = r.getJSONArray();
		r.close();
		return res;
	}
	
	public JSONArray pullChats(int chatroom, int timestamp) throws HttpResponseException, RestException, JSONException {
		Request r;
		Log.i("RPull", "pulling all chats...");
		r = new Request("/chat/get");
		try {
			r.putPost(new JSONObject()
					.put(GCM, "asdASDasdASD")
					.put(CHATROOM, chatroom)
					.put(TIMESTAMP, (timestamp == 0)? JSONObject.NULL : timestamp)
					.toString(), Pull.Mime.JSON);
		} catch (JSONException e) {
			Log.e("PullChat", "Unkown JSON error during POST");
		}
		JSONArray res = r.getJSONArray();
		r.close();
		return res;
	}


	public void setGcmId(String registrationId) {
		gcm = registrationId;
	}
	
	public void setGcmId() {
		setGcmId(GCMRegistrar.getRegistrationId(context));
	}
}
