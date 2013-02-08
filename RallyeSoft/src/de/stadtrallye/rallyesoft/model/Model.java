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
	
	private static Model model; // Singleton Pattern
	
	@SuppressWarnings("unused")
	private static boolean DEBUG = false;
	
	public enum Tasks { LOGIN, CHAT_DOWNLOAD, CHECK_SERVER, MAP_NODES, CHAT_REFRESH, LOGOUT, CHAT_POST };

	private SharedPreferences pref;
	RallyePull pull;
	private Context context;
	@SuppressWarnings("rawtypes") HashMap<AsyncRequest, Tasks> runningRequests;
	private String server;
	private int group;
	private String password;
	private String gcm;
	private ArrayList<IConnectionStatusListener> connectionListeners;
	
	private List<Chatroom> chatrooms;
	private ConnectionStatus connectionStatus;
	private IMapListener mapListener;
	private SQLiteDatabase db;
	
	public static Model getInstance(Context context, SharedPreferences pref, boolean loggedIn) {
		if (model != null)
			return model;
		else
			return model = new Model(context, pref, loggedIn);
	}
	
	private Model(Context context, SharedPreferences pref) {
		this(context, pref, false);
	}
	
	private Model(Context context, SharedPreferences pref, boolean loggedIn) {
		this.gcm = GCMRegistrar.getRegistrationId(context);
		this.pref = pref;
		this.context = context;
		this.connectionStatus = loggedIn? ConnectionStatus.Connected : ConnectionStatus.Disconnected;
		
		
		server = pref.getString(Std.SERVER, null);
		if (server == null) {
			connectionStatus = ConnectionStatus.Disconnected; //TODO: Implement "No Network" Status
			server = context.getString(R.string.default_server);
		} else {
			group = pref.getInt(Std.GROUP, 0);
			password = pref.getString(Std.PASSWORD, null);
			chatrooms = Chatroom.getChatrooms(extractChatRooms(pref.getString(Std.CHATROOMS, "")), this); //TODO: move to DB
		}
		
		pull = new RallyePull(server, gcm, context);
		
		runningRequests = new HashMap<AsyncRequest, Tasks>();
		connectionListeners = new ArrayList<IConnectionStatusListener>();
		
		SQLiteOpenHelper helper = new DatabaseOpenHelper(context);
		db = helper.getWritableDatabase();
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
	 * Converts JSONObject's to Integer values
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
	public void login(String server, String password, int group) {
		if (isLoggedIn()) {
			err.loggedIn();
			return;
		}
		try {
			startAsyncTask(Tasks.LOGIN,
					RallyePull.pendingLogin(context, server, group, password, gcm),
					new StringedJSONArrayConverter<Chatroom>(new LoginConverter()));
			
			connectionStatusChange(ConnectionStatus.Connecting);
			
			this.server = server;
			this.group = group;
			this.password = password;
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	@Override
	public void checkConnectionStatus() {
		try {
			startAsyncTask(Tasks.CHECK_SERVER, pull.pendingServerStatus(server), null);
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
	public String getServer() {
		return pref.getString(Std.SERVER, context.getString(R.string.default_server));
	}
	
	@Override
	public int getGroupId() {
		return pref.getInt(Std.GROUP, context.getResources().getInteger(R.integer.default_group));
	}
	
	public String getPassword() {
		return pref.getString(Std.PASSWORD, context.getString(R.string.default_password));
	}
	
	/**
	 * 
	 * WARNING: do not change List
	 */
	@Override
	public List<? extends IChatroom> getChatrooms() {
		return chatrooms;
	}
	
	public IChatroom getChatroom(int id) { //TODO: increase Efficiency
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
		String res = getServer() +"/pic/get/"+ pictureID +"/"+ size;
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
					connectionFailure(request.getException(), ConnectionStatus.Disconnected);
					this.logout(); //TODO: do retry's / currently logging out on default Server
				} else {
			
					chatrooms = (List<Chatroom>) request.get();
					
					saveLoginDetails(server, group, password, chatrooms);
					
					pull = new RallyePull(server, gcm, context);
					
					connectionStatusChange(ConnectionStatus.Connected);
				}
				
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
			break;
		case LOGOUT:
			connectionStatusChange(ConnectionStatus.Disconnected);
			break;
		case CHECK_SERVER:
			if (success) {
				ConnectionStatus state = (request.getResponseCode() >= 200 && request.getResponseCode() < 300)? ConnectionStatus.Connected : ConnectionStatus.Disconnected;
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
//				mapUpdate(); //TODO: Implement Separate Logic for MapListener
			} else
				err.asyncTaskResponseError(request.getException());
			break;
		default:
			Log.e(THIS, "Unknown Task callback: "+ request);
		}
		
		runningRequests.remove(request);
	}
	
	public void setMapListener(IMapListener l) {
		this.mapListener = l;
	}

	private void mapUpdate(List<MapNode> list) {
		mapListener.nodeUpdate(list);
	}

	private void saveLoginDetails(String server, int group, String password, List<Chatroom> chatrooms) {
		SharedPreferences.Editor edit = pref.edit();
//		edit.putBoolean(LOGGED_IN, success);
		edit.putString(Std.SERVER, server);
		edit.putInt(Std.GROUP, group);
		edit.putString(Std.PASSWORD, password);
		StringBuilder rooms = new StringBuilder();
		for (Chatroom c: chatrooms) {
			rooms.append(Integer.toString(c.getID())).append(';');
		}
		edit.putString(Std.CHATROOMS, rooms.toString());
		edit.commit();
	}

	/**
	 * Kill all AsyncTasks still running
	 */
	@Override
	public void onDestroy() {
		for (@SuppressWarnings("rawtypes") AsyncRequest ar: runningRequests.keySet())
		{
			ar.cancel(true);
		}
		
		runningRequests.clear();
	}
	
	private List<Integer> extractChatRooms(String string) {
		String rooms = pref.getString(Std.CHATROOMS, "");
		ArrayList<Integer> out = new ArrayList<Integer>();
		for (String s: rooms.split(";")) {
			try {
				out.add(Integer.parseInt(s));
			} catch (Exception e) {}
		}
		return out;
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
	
	public static void enableDebugLogging() {
		DEBUG = true;
		AsyncRequest.enableDebugLogging();
	}

	public ConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}
	
}
