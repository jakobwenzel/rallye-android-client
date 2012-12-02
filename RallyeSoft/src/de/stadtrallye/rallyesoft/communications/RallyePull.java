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

/**
 * REST Adapter
 * Creates PendingRequests for every type of communication with the server
 * @author Ray
 *
 */
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
		if (config.getString("server", null) == null) {
			Log.e("RallyePull", "No Configuration Found");
		}
		
		
		this.config = config;
		this.context = context;
		setGcmId();
	}
	
	
	public static RallyePull getPull(Context context) {
		RallyePull res = new RallyePull(context.getSharedPreferences(context.getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE), context);
		return res;
	}
	
//	public static String testConnection(String server) throws RestException, HttpResponseException {
//		Request r = new Pull(server).new Request("/getStatus");
//		return r.readLine();
//	}
	
	private static Request doLogin(Request r, String gcm, int group, String password) throws RestException {
		try {
			String post = new JSONObject()
			.put(GCM, gcm)
			.put(GROUP, group)
			.put(PASSWORD, password)
			.toString();
			r.putPost(post, Mime.JSON);
		} catch (JSONException e) {
			Log.e("RallyePull", "Login: Unkown JSON error during POST");
		}
		return r;
	}
	
	public JSONArray pushLogin() throws HttpResponseException, RestException, JSONException {
		return doLogin(new Request("/user/register"), gcm, config.getInt("group", 0), config.getString("password", "")).getJSONArray();
	}
	
	public static JSONArray pushLogin(Context context, String server, int group, String password) throws HttpResponseException, RestException, JSONException {
		return doLogin(new Pull(server).new Request("/user/register"), GCMRegistrar.getRegistrationId(context), group, password).getJSONArray();
	}
	
	public static PendingRequest pendingLogin(Context context, String server, int group, String password) throws RestException {
		return (PendingRequest) doLogin(new Pull(server).new PendingRequest("/user/register"), GCMRegistrar.getRegistrationId(context), group, password);
	}
	
	public String pushLogout() throws RestException, HttpResponseException {
		return pendingLogout().readLine();
	}
	
	public PendingRequest pendingLogout() throws RestException {
		PendingRequest r = new PendingRequest("/user/unregister");
		try {
			r.putPost(new JSONObject().put(GCM, gcm).toString(), Mime.JSON);
		} catch (JSONException e) {
			Log.e("RallyePull", "Logout: Unkown JSON error during POST");
		}
		return r;
	}

	public JSONArray pullAllNodes() throws HttpResponseException, RestException, JSONException {
		Log.i("RallyePull", "pulling all nodes...");
		Request r;
		r = new Request("/map/get/nodes");
		JSONArray res = r.getJSONArray();
		r.close();
		return res;
	}
	
	public JSONArray pullChats(int chatroom, int timestamp) throws HttpResponseException, RestException, JSONException {
		Request r;
		Log.i("RallyePull", "pulling all chats...");
		r = new Request("/chat/get");
		try {
			r.putPost(new JSONObject()
					.put(GCM, gcm)
					.put(CHATROOM, chatroom)
					.put(TIMESTAMP, (timestamp == 0)? JSONObject.NULL : timestamp)
					.toString(), Pull.Mime.JSON);
		} catch (JSONException e) {
			Log.e("RallyePull", "PullChats: Unkown JSON error during POST");
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
