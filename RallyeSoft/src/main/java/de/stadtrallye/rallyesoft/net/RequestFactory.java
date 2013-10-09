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
import de.rallye.model.structures.SimpleChatWithPictureHash;
import de.rallye.model.structures.SimpleSubmission;
import de.rallye.model.structures.SimpleSubmissionWithPictureHash;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.net.Request.RequestType;
import de.stadtrallye.rallyesoft.uimodel.IPicture;

public class RequestFactory {
	
	private static final String THIS = RequestFactory.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);

	private final ServerLogin login;
	private final String devId;
	private String pushId;
	
	public RequestFactory(ServerLogin login, String devId) {
		this.login = login;
		this.devId = devId;
		setAuth();
	}
	
	public void setPushID(String id) {
		this.pushId = id;
	}
	
	private void setAuth() {
		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				String realm = getRequestingPrompt();
				if (realm.equals("RallyeAuth")) {
					return new PasswordAuthentication(login.getHttpUser(), login.getUserPassword().toCharArray());
				} else if (realm.equals("RallyeNewUser")) {
					Log.i(THIS, "Switching to NewUserAuthentication");
					return new PasswordAuthentication(String.valueOf(login.getGroupID()), login.getGroupPassword().toCharArray());
				} else {
					return null;
				}
			}
		});
	}
	
	private URL getURL(String path) throws HttpRequestException {
		URL base = login.getServer();

		try {
			return new URL(base, path);
		} catch (MalformedURLException e) {
			throw err.MalformedURLError(e, base, path);
		}
	}
	
	public Request availableGroupsRequest() throws HttpRequestException {
		final URL url = getURL(Paths.GROUPS);
		return new Request(url);
	}
	
	public Request loginRequest() throws HttpRequestException {
		final URL url = getURL(Paths.GROUPS+"/"+login.getGroupID());
		Request r = new Request(url, RequestType.PUT);
		
		try {
			return r.putPost(new JSONObject()
					.put(LoginInfo.NAME, login.getName())
					.put(LoginInfo.UNIQUE_ID, devId)
					.put(LoginInfo.PUSH_MODE, "gcm")
					.put(LoginInfo.PUSH_ID, pushId));
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}
	
	public Request logoutRequest() throws HttpRequestException {
		final URL url = getURL(Paths.GROUPS+"/"+login.getGroupID()+"/"+login.getUserID());
		return new Request(url, RequestType.DELETE);
	}
	
	public Request serverStatusRequest() throws HttpRequestException {
		final URL url = getURL(Paths.STATUS);
		return new Request(url);
	}
	
	public Request availableChatroomsRequest() throws HttpRequestException {
		final URL url = getURL(Paths.CHATS);
		return new Request(url);
	}
	
	public Request chatRefreshRequest(int chatroom, long lastTime) throws HttpRequestException {
		final URL url = getURL(Paths.CHATS+"/"+chatroom+"/since/"+lastTime);
		return new Request(url);
	}

	public Request mapNodesRequest() throws HttpRequestException {
		final URL url = getURL(Paths.MAP_NODES);
		return new Request(url);
	}
	
	public Request mapEdgesRequest() throws HttpRequestException {
		final URL url = getURL(Paths.MAP_EDGES);
		return new Request(url);
	}

	public Request chatPostRequest(int chatroom, String msg, Integer pictureID) throws HttpRequestException {
		final URL url = getURL(Paths.CHATS+"/"+chatroom);
		Request r = new Request(url, RequestType.PUT);
		
		try {
			return r.putPost(new JSONObject()
					.put(SimpleChatEntry.MESSAGE, msg)
					.put(SimpleChatEntry.PICTURE_ID, (pictureID == null)? pictureID : JSONObject.NULL));
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}

	public Request chatPostWithHashRequest(int chatroom, String msg, String pictureHash) throws HttpRequestException {
		final URL url = getURL(Paths.CHATS+"/"+chatroom);
		Request r = new Request(url, RequestType.PUT);

		try {
			return r.putPost(new JSONObject()
					.put(SimpleChatEntry.MESSAGE, msg)
					.put(SimpleChatEntry.PICTURE_ID, JSONObject.NULL)
					.put(SimpleChatWithPictureHash.PICTURE_HASH, pictureHash));
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}

	public Request mapConfigRequest() throws HttpRequestException {
		final URL url = getURL(Paths.MAP_CONFIG);
		return new Request(url);
	}
	
	public URL getPictureUploadURL(String hash) {
		try {
			return getURL(Paths.PICS+"/"+hash);
		} catch (HttpRequestException e) {
			Log.e(THIS, "URL", e);
			return null;
		}
	}

	public Request serverInfoRequest() throws HttpRequestException {
		final URL url = getURL(Paths.INFO);
		return new Request(url);
	}

	public Request allUsersRequest() throws HttpRequestException {
		final URL url = getURL(Paths.USERS);
		return new Request(url);
	}

	public Request allTasksRequest() throws HttpRequestException {
		final URL url = getURL(Paths.TASKS);
		return new Request(url);
	}

	public Request allSubmissionsRequest(int groupID) throws HttpRequestException {
		final URL url = getURL(Paths.SUBMISSIONS+"/"+groupID);
		return new Request(url);
	}

	public Request submitSolutionRequest(int taskID, int type, IPicture picture, String text, String number) throws HttpRequestException {
		final URL url = getURL(Paths.TASKS+"/"+taskID);

		Request r = new Request(url, RequestType.PUT);

		try {
			JSONObject obj = new JSONObject()
					.put(SimpleSubmission.SUBMIT_TYPE, type)
					.put(SimpleSubmission.TEXT_SUBMISSION, text)
					.put(SimpleSubmission.INT_SUBMISSION, number);
			if (picture!=null)
					obj.put(SimpleSubmissionWithPictureHash.PICTURE_HASH,picture.getHash());
			r.putPost(obj);
			return r;
		} catch (JSONException e) {
			throw err.JSONDuringRequestCreationError(e, url);
		}
	}
}
