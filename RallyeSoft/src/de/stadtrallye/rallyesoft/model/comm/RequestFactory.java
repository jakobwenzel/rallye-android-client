package de.stadtrallye.rallyesoft.model.comm;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.structures.Login;

public class RequestFactory {
	
	private static final String THIS = RequestFactory.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);

	private URL base;
	private String devId;
	private String idName;
	
	public RequestFactory(URL baseURL, String idName) {
		base = baseURL;
		this.idName = idName;
	}
	
	public void setBaseURL(URL baseURL) {
		base = baseURL;
	}
	
	public void setDeviceID(String id) {
		this.devId = id;
	}
	
	public void setAuth(final Login login) {
		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(login.getId() +"@"+ login.group, login.password.toCharArray());
			}
		});
	}
	
	private JSONObject getDevIdJsonObject() throws JSONException {
		return new JSONObject().put(idName, devId);
	}
	
	private URL getURL(String path) throws HttpRequestException {
		try {
			return new URL(base, path);
		} catch (MalformedURLException e) {
			throw err.MalformedURLError(e, base, path);
		}
	}
	
	public Request loginRequest(Login login) throws HttpRequestException {
		final URL url = getURL(Paths.REGISTER);
		Request r = new Request(url);
		
		try {
			JSONObject post = getDevIdJsonObject()
				.put(Std.GROUPID, login.group)
				.put(Std.PASSWORD, login.password);
			r.putPost(post);
			return r;
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}
	
	public Request logoutRequest() throws HttpRequestException {
		final URL url = getURL(Paths.UNREGISTER);
		Request r = new Request(url);
		
		try {
			r.putPost(getDevIdJsonObject());
			return r;
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}
	
	public Request serverStatusRequest() throws HttpRequestException {
		final URL url = getURL(Paths.STATUS);
		Request r = new Request(url);
		
		try {
			r.putPost(getDevIdJsonObject());
			return r;
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}
	
	public Request chatRefreshRequest(int chatroom, long lastTime) throws HttpRequestException {
		final URL url = getURL(Paths.CHAT_READ);
		Request r = new Request(url);
		
		try {
			r.putPost(getDevIdJsonObject()
				.put(Std.CHATROOM, chatroom)
				.put(Std.TIMESTAMP, (lastTime == 0)? JSONObject.NULL : lastTime));
			return r;
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
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
		final URL url = getURL(Paths.CHAT_POST);
		Request r = new Request(url);
		
		try {
			r.putPost(getDevIdJsonObject()
				.put(Std.CHATROOM, chatroom)
				.put(Std.MSG, (msg.length()>0)? msg : JSONObject.NULL)
				.put(Std.PIC, (pictureID > 0)? pictureID : JSONObject.NULL));
			return r;
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}
	
	public Request groupListRequest() throws HttpRequestException {
		final URL url = getURL(Paths.GROUP_LIST);
		Request r = new Request(url);
		
		return r;
	}

	public Request serverConfigRequest() throws HttpRequestException {
		final URL url = getURL(Paths.CONFIG);
		Request r = new Request(url);
		return r;
	}
}
