package de.stadtrallye.rallyesoft.model.comm;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import de.stadtrallye.rallyesoft.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.structures.Login;

/**
 * REST Adapter
 * Creates PendingRequests for every type of communication with the server
 * @author Ray
 *
 */
public class RallyePull extends Pull {
	
	private static final String THIS = RallyePull.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);
	
	private String gcm;
	
	public RallyePull(String gcm) {
		this.gcm = gcm;
		
		if (gcm == null || gcm.length() == 0)
			Log.e(THIS, "Empty GCM ID");
	}
	
	public PendingRequest pendingLogin(Login login, String gcm) throws RestException { //TODO:use this.gcm
		final String rest = "/user/register";
		PendingRequest r = new PendingRequest(rest);
		try {
			String post = new JSONObject()
			.put(Std.GCM, gcm)
			.put(Std.GROUPID, login.getGroup())
			.put(Std.PASSWORD, login.getPassword())
			.toString();
			r.putPost(post, Mime.JSON);
		} catch (JSONException e) {
			throw err.JSONDuringPostError(e, rest);
		}
		return r;
	}
	
	public PendingRequest pendingLogout() throws RestException {
		final String rest = "/user/unregister";
		PendingRequest r = new PendingRequest(rest);
		try {
			r.putPost(new JSONObject().put(Std.GCM, gcm).toString(), Mime.JSON);
		} catch (JSONException e) {
			throw err.JSONDuringPostError(e, rest);
		}
		return r;
	}
	
	public PendingRequest pendingServerStatus() throws RestException {
		final String rest = "/system/status";
		PendingRequest r = new PendingRequest(rest);
		try {
			r.putPost(new JSONObject().put(Std.GCM, gcm).toString(), Mime.JSON);
		} catch (JSONException e) {
			throw err.JSONDuringPostError(e, rest);
		}
		return r;
	}

//	public JSONArray pullAllNodes() throws HttpResponseException, RestException, JSONException {
//		if (DEBUG)
//			Log.i(THIS, "pulling all nodes...");
//		Request r;
//		r = new Request("/map/nodes");
//		JSONArray res = r.getJSONArray();
//		r.close();
//		return res;
//	}
	
//	public JSONArray pullChats(int chatroom, int timestamp) throws HttpResponseException, RestException, JSONException {
//		Request r;
//		if (DEBUG)
//			Log.i(THIS, "pulling all chats...");
//		r = new Request("/chat/get");
//		try {
//			r.putPost(new JSONObject()
//					.put(GCM, gcm)
//					.put(CHATROOM, chatroom)
//					.put(TIMESTAMP, (timestamp == 0)? JSONObject.NULL : timestamp)
//					.toString(), Pull.Mime.JSON);
//		} catch (JSONException e) {
//			Log.e(THIS, "PullChats: Unkown JSON error during POST");
//		}
//		JSONArray res = r.getJSONArray();
//		r.close();
//		return res;
//	}
	
	public PendingRequest pendingChatRefresh(int chatroom, long lastTime) throws RestException {
		final String rest = "/chat/get";
		PendingRequest r = new PendingRequest(rest);
		try {
			r.putPost(new JSONObject()
			.put(Std.GCM, gcm)
			.put(Std.CHATROOM, chatroom)
			.put(Std.TIMESTAMP, (lastTime == 0)? JSONObject.NULL : lastTime)
			.toString(), Pull.Mime.JSON);
		} catch (JSONException e) {
			throw err.JSONDuringPostError(e, rest);
		}
		return r;
	}

	public PendingRequest pendingMapNodes() throws RestException {
		if (DEBUG)
			Log.i(THIS, "pulling all nodes...");
		final PendingRequest r;
		r = new PendingRequest("/map/nodes");
		return r;
	}

	public PendingRequest pendingChatPost(int chatroom, String msg, int pictureID) throws RestException {
		final String rest = "/chat/add";
		PendingRequest r = new PendingRequest(rest);
		try {
			r.putPost(new JSONObject()
			.put(Std.GCM, gcm)
			.put(Std.CHATROOM, chatroom)
			.put(Std.MSG, (msg.length()>0)? msg : JSONObject.NULL)
			.put(Std.PIC, (pictureID > 0)? pictureID : JSONObject.NULL)
			.toString(), Pull.Mime.JSON);
		} catch (JSONException e) {
			throw err.JSONDuringPostError(e, rest);
		}
		return r;
	}
}
