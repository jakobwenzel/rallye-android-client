package de.stadtrallye.rallyesoft.net;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import de.rallye.model.structures.LoginInfo;
import de.rallye.model.structures.SimpleChatEntry;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.structures.RallyeAuth;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.net.Request.RequestType;

public class RequestFactory implements IAuthManager {
	
	private static final String THIS = RequestFactory.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);

	private URL base;
	private String devId;
	private String pushId;
//	private String idName;
	private RallyeAuth rallyeAuth;
	private ServerLogin login;
	
	public RequestFactory(URL baseURL, String devId) {
		base = baseURL;
		this.devId = devId;
		setAuth();
	}
	
	public void setBaseURL(URL baseURL) {
		base = baseURL;
	}
	
	public void setPushID(String id) {
		this.pushId = id;
	}
	
	@Override
	public void setUserAuth(RallyeAuth rallyeAuth) {
		this.rallyeAuth = rallyeAuth;
	}
	
	private void setAuth() {
		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				String realm = getRequestingPrompt();
				if (realm.equals("RallyeAuth")) {
					return new PasswordAuthentication(rallyeAuth.getHttpUser(), rallyeAuth.getPassword());
				} else if (realm.equals("RallyeNewUser")) {
					Log.i(THIS, "Switching to NewUserAuthentication");
					return new PasswordAuthentication(String.valueOf(login.groupID), login.groupPassword.toCharArray());
				} else {
					return null;
				}
			}
		});
	}
	
	private URL getURL(String path) throws HttpRequestException {
		try {
			return new URL(base, path);
		} catch (MalformedURLException e) {
			throw err.MalformedURLError(e, base, path);
		}
	}
	
	public Request availableGroupsRequest() throws HttpRequestException {
		final URL url = getURL(Paths.GROUPS);
		Request r = new Request(url);
		
		return r;
	}
	
	public Request loginRequest(ServerLogin login) throws HttpRequestException {
		this.login = login;
		
		final URL url = getURL(Paths.GROUPS+"/"+login.groupID);
		Request r = new Request(url, RequestType.PUT);
		
		try {
			JSONObject post = new JSONObject()
				.put(LoginInfo.NAME, login.name)
				.put(LoginInfo.UNIQUE_ID, devId)
				.put(LoginInfo.PUSH_MODE, "gcm")
				.put(LoginInfo.PUSH_ID, pushId);
			r.putPost(post);
			return r;
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}
	
	public Request logoutRequest() throws HttpRequestException {
		final URL url = getURL(Paths.GROUPS+"/"+rallyeAuth.getGroupID()+"/"+rallyeAuth.userID);
		Request r = new Request(url, RequestType.DELETE);
		
		return r;
	}
	
	public Request serverStatusRequest() throws HttpRequestException {
		final URL url = getURL(Paths.STATUS);
		Request r = new Request(url);
		
		return r;
	}
	
	public Request availableChatroomsRequest() throws HttpRequestException {
		final URL url = getURL(Paths.CHATS);
		Request r = new Request(url);
		
		return r;
	}
	
	public Request chatRefreshRequest(int chatroom, long lastTime) throws HttpRequestException {
		final URL url = getURL(Paths.CHATS+"/"+chatroom+"/since/"+lastTime);
		Request r = new Request(url);
		
		return r;
	}

	public Request mapNodesRequest() throws HttpRequestException {
		final URL url = getURL(Paths.MAP_NODES);
		Request r = new Request(url);
		
		return r;
	}
	
	public Request mapEdgesRequest() throws HttpRequestException {
		final URL url = getURL(Paths.MAP_EDGES);
		Request r = new Request(url);
		
		return r;
	}

	public Request chatPostRequest(int chatroom, String msg, int pictureID) throws HttpRequestException {
		final URL url = getURL(Paths.CHATS+"/"+chatroom);
		Request r = new Request(url);
		
		try {
			r.putPost(new JSONObject()
				.put(SimpleChatEntry.MESSAGE, msg)
				.put(SimpleChatEntry.PICTURE_ID, (pictureID > 0)? pictureID : JSONObject.NULL));
			return r;
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}

	public Request serverConfigRequest() throws HttpRequestException {
		final URL url = getURL(Paths.CONFIG);
		Request r = new Request(url);
		return r;
	}
}
