package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.backend.DatabaseOpenHelper;
import de.stadtrallye.rallyesoft.model.backend.DatabaseOpenHelper.Groups;
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
	
	
	public enum Tasks { LOGIN, CHECK_SERVER, MAP_NODES, CHAT_REFRESH, LOGOUT, CHAT_POST };
	
	
	private static Model model; // Singleton Pattern

	
	private SharedPreferences pref;
	private RallyePull pull;
	private Context context;
	
	private Login currentLogin;
	private Login newLogin;
	
	@SuppressWarnings("rawtypes")
	private HashMap<AsyncRequest, Tasks> runningRequests = new HashMap<AsyncRequest, Tasks>();
	
	private ConnectionStatus connectionStatus = ConnectionStatus.Unknown;
	private ArrayList<IConnectionStatusListener> connectionListeners  = new ArrayList<IConnectionStatusListener>();
	private IMapListener mapListener;
	
	private List<Chatroom> chatrooms;
	
	private SQLiteDatabase db;
	
	
	public static Model getInstance(Context context, boolean loggedIn) {//TODO: detect rotate/ prevent killing model during rotate
		if (model != null)
			return model.ensureConnections();
		else
			return model = new Model(context, getDefaultPreferences(context), loggedIn);
	}
	
	private Model ensureConnections() {
		db = new DatabaseOpenHelper(context).getWritableDatabase();
		
		return model;
	}
	
	private static SharedPreferences getDefaultPreferences(Context context) {
		return context.getSharedPreferences(context.getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
	}
	
	private Model(Context context, SharedPreferences pref, boolean loggedIn) {
		String gcm = GCMRegistrar.getRegistrationId(context);
		this.pref = pref;
		this.context = context;
		this.connectionStatus = loggedIn? ConnectionStatus.Connected : ConnectionStatus.Unknown;
		
		pull = new RallyePull(gcm);
		
		SQLiteOpenHelper helper = new DatabaseOpenHelper(context);
		db = helper.getWritableDatabase();
		
		currentLogin = readLogin();
		
		if (connectionStatus == ConnectionStatus.Connected) {
			Log.i(THIS, "Warm Start: assuming previously logged in!");
			
			pull.setBaseURL(currentLogin.getServer());
		} else {
			if (currentLogin.isValid()) {
				if (isDbSynchronized()) {
					Log.i(THIS, "Cold Start: Found Login-Data and DB -> checking connection");
					connectionStatus = ConnectionStatus.Connecting;
					
					chatrooms = Chatroom.getChatrooms(this);
					
					pull.setBaseURL(currentLogin.getServer());
					
					checkConnectionStatus();
				} else {
					Log.w(THIS, "Cold Start: Found Login-Data, but DB incompatible -> Log In");
					connectionStatus = ConnectionStatus.Retrying;
					
					pull.setBaseURL(currentLogin.getServer());
					
					newLogin = currentLogin;
					logout(); //During Retrying after logout completed a new attempt at login will be started at newLogin
				}
			} else {
				Log.w(THIS, "No usable Login-Data found -> stay offline");
				connectionStatus = ConnectionStatus.Disconnected; //TODO: Implement "No Network" Status
				
				currentLogin = null;
			}
		}
	}
	
	private boolean isDbSynchronized() {
		Cursor c = db.query(Groups.TABLE, new String[]{Groups.KEY_NAME}, Groups.KEY_ID+"="+currentLogin.getGroup(), null, null, null, null);
		if (c.getCount() != 1)
			return false;
		else
			return true;
	}

	@SuppressWarnings("rawtypes")
	HashMap<AsyncRequest, Tasks> getRunningRequests() {
		return runningRequests;
	}
	
	RallyePull getRallyePull() {
		return pull;
	}
	
	SQLiteDatabase getDb() {
		return db;
	}
	
	
	@Override
	public void logout() {
		if (currentLogin == null && newLogin == null) {
			Log.e(THIS, "Cannot logout: no server specified");
			connectionFailure(new Exception("Cannot logout: no server specified"), ConnectionStatus.Unknown);
			return;
		}
		
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
				String gcm = GCMRegistrar.getRegistrationId(context);
				pull = new RallyePull(gcm);
				pull.setBaseURL(login.getServer());
				
				startAsyncTask(Tasks.LOGIN,
						pull.pendingLogin(newLogin, gcm),
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
			startAsyncTask(Tasks.CHECK_SERVER, pull.pendingServerStatus(), null);
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
	
	protected <T> AsyncRequest<T> startAsyncTask(IAsyncFinished callback, Tasks taskType, PendingRequest payload, IConverter<String, T> converter) {
		AsyncRequest<T> ar = new AsyncRequest<T>(callback, converter);
		runningRequests.put(ar, taskType);
		ar.execute(payload);
		return ar;
	}
	
	private <T> AsyncRequest<T> startAsyncTask(Tasks taskType, PendingRequest payload, IConverter<String, T> converter) {
		return startAsyncTask(this, taskType, payload, converter);
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
	public void onAsyncFinished(@SuppressWarnings("rawtypes") final AsyncRequest request, final boolean success) {
		Tasks type = runningRequests.get(request);
		
		if (type == null && !success) {
			Log.w(THIS, "Task Callback with type 'null' => cancelled Task");
			return;
		} else if (type == null) {
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
						newLogin = currentLogin = null;
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
			if (success) {//TODO: get rid of throw Except
				ConnectionStatus state = (request.getResponseCode() >= 200 && request.getResponseCode() < 300)? ConnectionStatus.Connected : ConnectionStatus.Disconnected;
				currentLogin.validated();
				connectionStatusChange(state);
			} else
				connectionStatusChange(ConnectionStatus.Disconnected);
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
//		edit.putString(Std.CHATROOMS, serializeChatroomIds());
		
		ContentValues insert = new ContentValues();
		insert.put(Groups.KEY_ID, currentLogin.getGroup());
		//TODO: put Name
		db.insert(Groups.TABLE, null, insert);
		
		for (Chatroom c: chatrooms) {
			c.writeToDb();
		}
		
		edit.commit();
	}
	
	private Login readLogin() {
		return new Login(pref.getString(Std.SERVER, null),
				pref.getInt(Std.GROUP, 0),
				pref.getString(Std.PASSWORD, null),
				pref.getLong(Std.LAST_LOGIN, 0));
	}
	
//	@Deprecated
//	private void readChatRooms() { //TODO: move to DB
//		String rooms = pref.getString(Std.CHATROOMS, "");
//		chatrooms = Chatroom.getChatrooms(rooms, this);
//	}
	
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
		
		db.close();
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
		if (connectionStatus == ConnectionStatus.Retrying && (newState != ConnectionStatus.Disconnected && newState != ConnectionStatus.Connected)) {
			Log.i(THIS, "Status: Ignoring change to "+ newState +" while retrying");
			return;
		}
		
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
