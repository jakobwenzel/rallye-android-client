package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.backend.DatabaseOpenHelper;
import de.stadtrallye.rallyesoft.model.comm.AsyncRequest;
import de.stadtrallye.rallyesoft.model.comm.RallyePull;
import de.stadtrallye.rallyesoft.model.comm.Pull.PendingRequest;
import de.stadtrallye.rallyesoft.model.structures.Login;
import de.stadtrallye.rallyesoft.model.structures.MapNode;
import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONConverter;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayConverter;

/**
 * My Model
 * Should be the only Class to write to Preferences
 * All Tasks should start here
 * @author Ray
 *
 */
public class Model implements IModel, IAsyncFinished {
	
	private static final String THIS = Model.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);
	@SuppressWarnings("unused")
	private static boolean DEBUG = false;
	
	
	public enum Tasks { LOGIN, CHAT_DOWNLOAD, CHECK_SERVER, MAP_NODES, CHAT_REFRESH, LOGOUT, CHAT_POST };
	
	
	private static Model model; // Singleton Pattern

	
	private SharedPreferences pref;
	private RallyePull pull;
	private Context context;
	
	private Login currentLogin;
	private Login newLogin;
	
	@SuppressWarnings("rawtypes")
	private HashMap<AsyncRequest, Tasks> runningRequests;
	private ArrayList<IConnectionStatusListener> connectionListeners;
	private IMapListener mapListener;
	private ConnectionStatus connectionStatus;
	
	private List<Chatroom> chatrooms;
	
	private SQLiteDatabase db;
	
	
	public static Model getInstance(Context context, boolean loggedIn) {
		if (model != null)
			return model;
		else
			return model = new Model(context, getDefaultPreferences(context), loggedIn);
	}
	
	private static SharedPreferences getDefaultPreferences(Context context) {
		return context.getSharedPreferences(context.getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
	}
	
	private Model(Context context, SharedPreferences pref, boolean loggedIn) {
		String gcm = GCMRegistrar.getRegistrationId(context);
		this.pref = pref;
		this.context = context;
		this.connectionStatus = loggedIn? ConnectionStatus.Connected : ConnectionStatus.Disconnected;
		
		
		currentLogin = readLogin();
		if (!currentLogin.isComplete()) {
			connectionStatus = ConnectionStatus.Disconnected; //TODO: Implement "No Network" Status
//			currentLogin = getDefaultLogin(context);
		} else {
			readChatRooms(); //TODO: move to DB
		}
		
		pull = new RallyePull(currentLogin.getServer(), gcm);
		
		runningRequests = new HashMap<AsyncRequest, Tasks>();
		connectionListeners = new ArrayList<IConnectionStatusListener>();
		
		SQLiteOpenHelper helper = new DatabaseOpenHelper(context);
		db = helper.getWritableDatabase();
	}
	
	HashMap<AsyncRequest, Tasks> getRunningRequests() {
		return runningRequests;
	}
	
	RallyePull getRallyePull() {
		return pull;
	}
	
	
	@Override
	public void logout() {
		try {
			startAsyncTask(Tasks.LOGOUT, pull.pendingLogout(), null);
			connectionStatusChange(ConnectionStatus.Disconnecting);
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	/**
	 * Converts JSONObject's to Chatrooms
	 * 
	 * used in conjunction with {@link StringedJSONArrayConverter}
	 * @author Ramon
	 */
	private static class LoginConverter extends JSONConverter<Chatroom> {
		//TODO: put universal Converters in separate files / packages 
		@Override
		public Chatroom doConvert(JSONObject o) throws JSONException {
			int i = o.getInt("chatroom");
			String name;
			
			try {
				name = o.getString("name");
			} catch (Exception e) {
				name = "Chatroom "+ i;
			}
			
			return new Chatroom(i, name, model);
		}
	}
	
	@Override
	public void login(Login login) {
		if (isLoggedIn()) {
			err.loggedIn();
			return;
		}
		
		newLogin = login;
		 
		if (newLogin.isComplete()) {
			try {
				
				startAsyncTask(Tasks.LOGIN,
						RallyePull.pendingLogin(newLogin, GCMRegistrar.getRegistrationId(context)),
						new StringedJSONArrayConverter<Chatroom>(new LoginConverter()));
				
				connectionStatusChange(ConnectionStatus.Connecting);
				
			} catch (RestException e) {
				err.restError(e);
			}
		} else {
			err.loginInvalid(newLogin);
		}
	}
	
	@Override
	public void checkConnectionStatus() {
		try {
			startAsyncTask(Tasks.CHECK_SERVER, pull.pendingServerStatus(currentLogin.getServer()), null);
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	//TODO: always precache / refresh on mapNeeded??
	public void getMapNodes() {
		if (!isLoggedIn()) {
			err.notLoggedIn();
			return;
		}
		try {
			startAsyncTask(Tasks.MAP_NODES, pull.pendingMapNodes(), new StringedJSONArrayConverter<MapNode>(new MapNode.NodeConverter()));
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	protected <T> void startAsyncTask(IAsyncFinished callback, Tasks taskType, PendingRequest payload, IConverter<String, T> converter) {
		AsyncRequest<T> ar = new AsyncRequest<T>(callback, converter);
		runningRequests.put(ar, taskType);
		ar.execute(payload);
	}
	
	private <T> void startAsyncTask(Tasks taskType, PendingRequest payload, IConverter<String, T> converter) {
		startAsyncTask(this, taskType, payload, converter);
	}
	
	@Override
	public boolean isLoggedIn() {
		return connectionStatus == ConnectionStatus.Connected;
	}
	
	@Override
	public Login getLogin() {
		return (currentLogin != null)? currentLogin : getDefaultLogin();
	}
	
	/**
	 * 
	 * WARNING: do not change List
	 */
	@Override
	public List<? extends IChatroom> getChatrooms() {
		return chatrooms;
	}
	
	
	/**
	 * Returns IChatroom with id
	 * 
	 * Will iterate through all chatrooms until found or null
	 */
	public IChatroom getChatroom(int id) {
		for (IChatroom r: chatrooms) {
			if (r.getID() == id)
			{
				return r;
			}
		}
		
		return null;
	}
	
	
	@Override
	public String getUrlFromImageId(int pictureID, char size) {
		String res = currentLogin.getServer() +"/pic/get/"+ pictureID +"/"+ size;
//		Log.v(THIS, res);
		return res;
	}

	
	@Override
	public void onAsyncFinished(final AsyncRequest request, final boolean success) {
		Tasks type = runningRequests.get(request);
		
		if (type == null) {
			Log.e(THIS, "Task Callback with type 'null'");
			return;
		}
		
		
		switch (type) {
		case LOGIN:
			try {
				if (!success) {
					if (!isRetrying()) { //TODO: only when unauthorized by server because of previous login (subject to change)
						connectionStatusChange(ConnectionStatus.Retrying);
						this.logout();
					} else {
						connectionFailure(request.getException(), ConnectionStatus.Disconnected);
					}
				} else {
			
					chatrooms = (List<Chatroom>) request.get();
					
					newLogin.validated();
					currentLogin = newLogin;
					newLogin = null;
					
					writeLoginAndChatrooms();
					
					connectionStatusChange(ConnectionStatus.Connected);
				}
				
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
			break;
		case LOGOUT:
			if (isRetrying()) {
				this.login(newLogin);
			} else
				connectionStatusChange(ConnectionStatus.Disconnected);
			
			break;
		case CHECK_SERVER:
			if (success) {
				ConnectionStatus state = (request.getResponseCode() >= 200 && request.getResponseCode() < 300)? ConnectionStatus.Connected : ConnectionStatus.Disconnected;
				currentLogin.validated();
				connectionStatusChange(state);
			} else
				err.asyncTaskResponseError(request.getException());
			break;
		case MAP_NODES:
			if (success) {
				try {
					mapUpdate((List<MapNode>)request.get());
				} catch (Exception e) {
					err.asyncTaskResponseError(e);
				}
			} else
				err.asyncTaskResponseError(request.getException());
			break;
		default:
			Log.e(THIS, "Unknown Task callback: "+ request);
		}
		
		runningRequests.remove(request);
	}
	
	@Deprecated
	private String serializeChatroomIds() {
		StringBuilder rooms = new StringBuilder();
		
		for (Chatroom c: chatrooms) {
			rooms.append(Integer.toString(c.getID())).append(';');
		}
		
		return rooms.toString();
	}

	private void writeLoginAndChatrooms() {
		SharedPreferences.Editor edit = pref.edit();
		edit.putLong(Std.LAST_LOGIN, currentLogin.getLastValidated());
//		edit.putLong(Std.LAST_CONTACT, time);
		edit.putString(Std.SERVER, currentLogin.getServer());
		edit.putInt(Std.GROUP, currentLogin.getGroup());
		edit.putString(Std.PASSWORD, currentLogin.getPassword());
		edit.putString(Std.CHATROOMS, serializeChatroomIds());
		edit.commit();
	}
	
	private Login readLogin() {
		return new Login(pref.getString(Std.SERVER, null),
				pref.getInt(Std.GROUP, 0),
				pref.getString(Std.PASSWORD, null),
				pref.getLong(Std.LAST_LOGIN, 0));
	}
	
	private void readChatRooms() {
		String rooms = pref.getString(Std.CHATROOMS, "");
		chatrooms = Chatroom.getChatrooms(rooms, model);
	}
	
	private Login getDefaultLogin() {
		return new Login(context.getString(R.string.default_server),
				Integer.valueOf(context.getString(R.string.default_group)),
				context.getString(R.string.default_password));
	}
	
	
	@Override
	public void onStop() {
		for (Chatroom c: chatrooms) {
			c.onStop();
		}
	}
	
	/**
	 * Kill all AsyncTasks still running
	 */
	@Override
	public void onDestroy() {
		for (Chatroom c: chatrooms) {
			c.onDestroy();
		}
		
		for (@SuppressWarnings("rawtypes") AsyncRequest ar: runningRequests.keySet())
		{
			ar.cancel(true);
		}
		
		runningRequests.clear();
	}
	
	public void setMapListener(IMapListener l) {
		this.mapListener = l;
	}

	private void mapUpdate(List<MapNode> list) {
		mapListener.nodeUpdate(list);
	}
	
	@Override
	public void addListener(IConnectionStatusListener l) {
		connectionListeners.add(l);
	}
	
	@Override
	public void removeListener(IConnectionStatusListener l) {
		connectionListeners.remove(l);
	}
	
	private void connectionStatusChange(ConnectionStatus newState) {
		if (connectionStatus == ConnectionStatus.Retrying && (newState != ConnectionStatus.Disconnected || newState != ConnectionStatus.Connected))
		
		connectionStatus = newState;
		
		Log.i(THIS, "Status: "+ newState);
		
		for(IConnectionStatusListener l: connectionListeners) {
			l.onConnectionStatusChange(newState);
		}
	}
	
	private void connectionFailure(Exception e, ConnectionStatus fallbackState) {
		connectionStatus = fallbackState;
		
		Log.e(THIS, e +"\n fallback: "+ fallbackState);
		
		for(IConnectionStatusListener l: connectionListeners) {
			l.onConnectionFailed(e, fallbackState);
		}
	}
	
	private boolean isRetrying() {
		return connectionStatus == ConnectionStatus.Retrying;
	}
	
	public ConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}
	
	public static void enableDebugLogging() {
		DEBUG = true;
		AsyncRequest.enableDebugLogging();
	}
	
}
