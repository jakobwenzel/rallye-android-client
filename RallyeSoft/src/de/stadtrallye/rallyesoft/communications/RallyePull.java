package de.stadtrallye.rallyesoft.communications;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;

public class RallyePull extends Pull {
	
	private String gcm = "";
	private Context context;
	private SharedPreferences config;
	
	private final static String GCM = "gcm";
	private final static String TIMESTAMP = "timestamp";
	private final static String CHATROOM = "chatroom";
	private final static String PASSWORD = "password";
	private final static String GROUP = "groupID";
	
	public RallyePull(SharedPreferences config, Context context) {
		super(config.getString("server", null));
		
		this.config = config;
		this.context = context;
	}
	
	
	public static RallyePull getPull(Context context) {
		RallyePull res = new RallyePull(context.getSharedPreferences(context.getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE), context);
		return res;
	}
	
	public static String testConnection(String server) throws RestException, HttpResponseException {
		Request r = new Pull(server).new Request("/getStatus");
		return r.readLine();
	}
	
	
	public JSONArray pushLogin() throws HttpResponseException, RestException, JSONException {
		Request r = makeRequest("/user/register");
		try {
			String post = new JSONObject()
			.put(GCM, gcm)
			.put(GROUP, config.getInt("group", 0))
			.put(PASSWORD, config.getString("password", ""))
			.toString();
			Log.d("RLogin", post);
			r.putPost(post, Mime.JSON);
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
					.put(GCM, gcm)
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
