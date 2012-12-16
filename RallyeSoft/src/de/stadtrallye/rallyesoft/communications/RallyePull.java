package de.stadtrallye.rallyesoft.communications;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import de.stadtrallye.rallyesoft.exceptions.Err;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;

/**
 * REST Adapter
 * Creates PendingRequests for every type of communication with the server
 * @author Ray
 *
 */
public class RallyePull extends Pull {
	
	private static final String THIS = RallyePull.class.getSimpleName();
	
	private String gcm;
	private Context context;
	
	private final static String GCM = "gcm";
	private final static String TIMESTAMP = "timestamp";
	private final static String CHATROOM = "chatroom";
	private final static String PASSWORD = "password";
	private final static String GROUP = "groupID";
	
	public RallyePull(String server, String gcm, Context context) {
		super(server);
		
		this.context = context;
		this.gcm = gcm;
	}
	
	public static PendingRequest pendingLogin(Context context, String server, int group, String password, String gcm) throws RestException {
		final String rest = "/user/register";
		PendingRequest r = new Pull(server).new PendingRequest(rest);
		try {
			String post = new JSONObject()
			.put(GCM, gcm)
			.put(GROUP, group)
			.put(PASSWORD, password)
			.toString();
			r.putPost(post, Mime.JSON);
		} catch (JSONException e) {
			throw Err.JSONDuringPostError(e, rest);
		}
		return r;
	}
	
	public PendingRequest pendingLogout() throws RestException {
		final String rest = "/user/unregister";
		PendingRequest r = new PendingRequest(rest);
		try {
			r.putPost(new JSONObject().put(GCM, gcm).toString(), Mime.JSON);
		} catch (JSONException e) {
			throw Err.JSONDuringPostError(e, rest);
		}
		return r;
	}
	
	public PendingRequest pendingServerStatus(String server) throws RestException {
		final String rest = "/system/status";
		PendingRequest r = new Pull(server).new PendingRequest(rest);
		try {
			r.putPost(new JSONObject().put(GCM, gcm).toString(), Mime.JSON);
		} catch (JSONException e) {
			throw Err.JSONDuringPostError(e, rest);
		}
		return r;
	}

	public JSONArray pullAllNodes() throws HttpResponseException, RestException, JSONException {
		if (DEBUG)
			Log.i(THIS, "pulling all nodes...");
		Request r;
		r = new Request("/map/nodes");
		JSONArray res = r.getJSONArray();
		r.close();
		return res;
	}
	
	public JSONArray pullChats(int chatroom, int timestamp) throws HttpResponseException, RestException, JSONException {
		Request r;
		if (DEBUG)
			Log.i(THIS, "pulling all chats...");
		r = new Request("/chat/get");
		try {
			r.putPost(new JSONObject()
					.put(GCM, gcm)
					.put(CHATROOM, chatroom)
					.put(TIMESTAMP, (timestamp == 0)? JSONObject.NULL : timestamp)
					.toString(), Pull.Mime.JSON);
		} catch (JSONException e) {
			Log.e(THIS, "PullChats: Unkown JSON error during POST");
		}
		JSONArray res = r.getJSONArray();
		r.close();
		return res;
	}
	
	public PendingRequest pendingChatRefresh(int chatroom, int timestamp) throws RestException {
		final String rest = "/chat/get";
		PendingRequest r = new PendingRequest(rest);
		try {
			r.putPost(new JSONObject()
			.put(GCM, gcm)
			.put(CHATROOM, chatroom)
			.put(TIMESTAMP, (timestamp == 0)? JSONObject.NULL : timestamp)
			.toString(), Pull.Mime.JSON);
		} catch (JSONException e) {
			throw Err.JSONDuringPostError(e, rest);
		}
		return r;
	}
}
