package de.stadtrallye.rallyesoft.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.model.structures.Group;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.structures.GroupUser;
import de.stadtrallye.rallyesoft.model.structures.RallyeAuth;
import de.stadtrallye.rallyesoft.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.model.structures.ServerConfig;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.exceptions.LoginFailedException;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.JSONObjectRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.LoginExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.net.Paths;
import de.stadtrallye.rallyesoft.net.RequestFactory;

/**
 * My Model
 * Should be the only Class to write to Preferences
 * All Tasks should start here
 * @author Ray
 *
 */
public class Model extends Binder implements IModel, RequestExecutor.Callback<Model.Tasks> {
	
	private static final String THIS = Model.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);
	@SuppressWarnings("unused")
	final private static boolean DEBUG = false;

	enum Tasks { LOGIN, CHECK_SERVER, MAP_NODES, LOGOUT, CONFIG, GROUP_LIST, SERVER_INFO, AVAILABLE_CHATROOMS }
	
	
	private static Model model; // Singleton Pattern

	
	final private SharedPreferences pref;
	final private Context context;
	
	private ServerLogin currentLogin;
	private RallyeAuth rallyeAuth;
	ServerConfig serverConfig;
	
	private ConnectionStatus status; //TODO: add network state
	final ArrayList<IConnectionStatusListener> connectionListeners  = new ArrayList<IConnectionStatusListener>();
	private IAvailableGroupsCallback availableGroupsCallback;
	private IServerInfoCallback serverInfoCallback;
	
	final Handler uiHandler = new Handler(Looper.getMainLooper());
	final RequestFactory factory;
	ExecutorService exec;
	
	private List<Chatroom> chatrooms;
	final private Map map;
	
	SQLiteDatabase db;
	
	/**
	 * Model can now be kept through Configuration Changes
	 * @param context needed for Database, Preferences, GCMid (and Res-Strings for default_login [not lang-specific]) => give ApplicationContext
	 * @return Singleton Model
	 */
	public static Model getInstance(Context context) {
		if (model != null) {
			return model.reuse();
		} else {
			return model = new Model(context, getDefaultPreferences(context));
		}
	}
	
	private static SharedPreferences getDefaultPreferences(Context context) {
		return context.getSharedPreferences(Std.CONFIG_MAIN, Context.MODE_PRIVATE);
	}
	
	private Model reuse() {
		if (!model.db.isOpen()) {
			initDatabase();
		}
		if (model.exec.isShutdown()) {
			initExecutor();
		}
		
		return this;
	}
	
	private void initExecutor() {
		exec = Executors.newCachedThreadPool();
	}
	
	private void initDatabase() {

		db = new DatabaseHelper(context).getWritableDatabase();
	}
	
	private Model(Context context, SharedPreferences pref) {
		this.pref = pref;
		this.context = context;
		
		initDatabase();

//        Log.w(THIS, "ID, name, groupID, lastUpdate, lastID");
//        Cursor c = db.query(false , Chatrooms.TABLE, null, null, null, null, null, null, null);
//        while(c.moveToNext()) {
//            Log.w(THIS, c.getInt(0)+","+c.getString(1)+","+c.getInt(2)+","+c.getLong(3)+","+c.getInt(4));
//        }

		initExecutor();
		
		restoreLogin();
		
		String uniqueID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		factory = new RequestFactory(null, uniqueID);
		
		try {
			if (currentLogin == null || !currentLogin.isComplete())
				throw new LoginFailedException();
				
			URL url = new URL(currentLogin.server);
			factory.setBaseURL(url);
			factory.setPushID(GCMRegistrar.getRegistrationId(context));
			factory.setUserAuth(rallyeAuth);
			
			
			restoreServerConfig();
			restoreConnectionStatus();
			
			try {
				chatrooms = Chatroom.getChatrooms(this);
			} catch (Exception e) {
				Log.e(THIS, "Chatrooms could not be restored");
			}
			
			if (serverConfig == null) {
				Log.e(THIS, "ServerConfig could not be restored");
				refreshServerConfig();
			}
		} catch (Exception e) {
			Log.w(THIS, "No usable Login-Data found -> stay offline", e);
			setConnectionStatus(ConnectionStatus.Disconnected);
		}
		
		map = new Map(this);

        checkConfiguration();
	}
	
	@Override
	public void logout() {
		if (currentLogin == null || !isConnected()) {
			Exception e = err.logoutInvalid();
			connectionFailure(e, ConnectionStatus.Disconnected);
			return;
		}
		
		setConnectionStatus(ConnectionStatus.Disconnecting);
		save().saveConnectionStatus(ConnectionStatus.Disconnected).commit();
		
		try {
			exec.execute(new RequestExecutor<String, Tasks>(factory.logoutRequest(), null, this, Tasks.LOGOUT));
		} catch (Exception e) {
			err.requestException(e);
		}
	}
	
	private void logoutResult(RequestExecutor<String, ?> r) {
		rallyeAuth = null;
		factory.setUserAuth(null);
		setConnectionStatus(ConnectionStatus.Disconnected);
	}
	
	@Override
	public void login(ServerLogin login) {
		synchronized (this) {
			if (!isDisconnected()) {
				if (isConnected()) {
					err.loggedIn();
				} else if (isConnectionChanging()) {
					err.concurrentConnectionChange("Login");
				} else if (!login.isComplete()) {
					err.loginInvalid(login);
				}
				return;
			} else if (login.equals(currentLogin)) {
				login = currentLogin;
			}//Sanity
			
			setConnectionStatus(ConnectionStatus.Connecting);
		}
		
		try {
			factory.setPushID(GCMRegistrar.getRegistrationId(context));
			factory.setBaseURL(new URL(login.server));
			
			exec.execute(new LoginExecutor<Tasks>(factory.loginRequest(login), login, this, Tasks.LOGIN));
		
		} catch (Exception e) {
			err.requestException(e);
			connectionFailure(e, ConnectionStatus.Disconnected);
		}
		
	}
	
	public void loginResult(LoginExecutor<?> r) {
		if (r.isSuccessful()) {
			
			rallyeAuth = r.getResult();
			factory.setUserAuth(rallyeAuth);
			currentLogin = r.getLogin();
			
			save().saveLogin().saveAuth().commit();
			
			getAvailableChatrooms();
			refreshServerConfig();
			map.updateMap();
		} else {
			connectionFailure(r.getException(), ConnectionStatus.Disconnected);
		}
	}
	
	/**
	 * Start checking for configuration changes
     */
	@Override
	public void checkConfiguration() {
		if (!isConnected()) {
			err.notLoggedIn();
			return;
		}

        getAvailableChatrooms();
        refreshServerConfig();
	}
	
	private void refreshServerConfig() {
		try {
			Log.d(THIS, "getting Server config");
			exec.execute(new JSONObjectRequestExecutor<ServerConfig, Tasks>(factory.serverConfigRequest(), new ServerConfig.ServerConfigConverter(), this, Tasks.CONFIG));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void serverConfigResult(RequestExecutor<ServerConfig, ?> r) {
		if (r.isSuccessful()) {
				ServerConfig serverConfig = r.getResult();

				if (!serverConfig.equals(this.serverConfig)) {
					this.serverConfig = serverConfig;
					save().saveServerConfig().commit();
				}

				checkConnectionComplete();
		} else {
			err.asyncTaskResponseError(r.getException());
		}
	}
	
	private void getAvailableChatrooms() {
		try {
			Log.d(THIS, "getting available chatrooms");
			exec.execute(new JSONArrayRequestExecutor<Chatroom, Tasks>(factory.availableChatroomsRequest(), new Chatroom.ChatroomConverter(this), this, Tasks.AVAILABLE_CHATROOMS));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void availableChatroomsResult(RequestExecutor<List<Chatroom>, ?> r) {
		if (r.isSuccessful()) {
				List<Chatroom> chatrooms = r.getResult();

                if (!chatrooms.equals(this.chatrooms)) {
                    this.chatrooms = chatrooms;
            	    save().saveChatrooms().commit();
                }

				checkConnectionComplete();
		} else {
			err.asyncTaskResponseError(r.getException());
		}
	}
	
	private void checkConnectionComplete() {
		if (status == ConnectionStatus.Connecting && chatrooms != null && chatrooms.size() > 0 && serverConfig != null) {
			setConnectionStatus(ConnectionStatus.Connected);
			save().saveConnectionStatus(status).commit();
		}
			
	}

	@Override
	public void getAvailableGroups(IAvailableGroupsCallback callback, String server) {
		if (!isConnected()) {
			try {
				factory.setBaseURL(new URL(server));
			} catch (MalformedURLException e) {
				Log.e(THIS, "Invalid URL");
			}
		}
		
		try {
			availableGroupsCallback = callback;
			exec.execute(new JSONArrayRequestExecutor<Group, Tasks>(factory.availableGroupsRequest(), new Group.GroupConverter(), this, Tasks.GROUP_LIST));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void availableGroupsResult(RequestExecutor<List<Group>, ?> r) {
		final List<Group> groups = r.getResult();

		if (availableGroupsCallback != null)
			uiHandler.post(new Runnable(){
				@Override
				public void run() {
					availableGroupsCallback.availableGroups(groups);
					availableGroupsCallback = null;
				}
			});
	}

	@Override
	public void getServerInfo(IServerInfoCallback callback, String server) {
		if (!isConnected()) {
			try {
				factory.setBaseURL(new URL(server));
			} catch (MalformedURLException e) {
				Log.e(THIS, "Invalid URL");
			}
		}

		try {
			serverInfoCallback = callback;
			exec.execute(new JSONObjectRequestExecutor<>(factory.serverInfoRequest(), new ServerInfo.Converter(), this, Tasks.SERVER_INFO));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}

	private void serverInfoResult(RequestExecutor<ServerInfo, ?> r) {
		final ServerInfo info = r.getResult();

		if (serverInfoCallback != null)
			uiHandler.post(new Runnable(){
				@Override
				public void run() {
					serverInfoCallback.serverInfo(info);
					serverInfoCallback = null;
				}
			});
	}
	
	public URL getPictureUploadURL(String hash) {
		return factory.getPictureUploadURL(hash);
	}
	
	@Override
	public boolean isConnectionChanging() {
		return status == ConnectionStatus.Connecting || status == ConnectionStatus.Disconnecting;
	}
	
	@Override
	public boolean isConnected() {
		return status == ConnectionStatus.Connected;
	}
	
	@Override
	public boolean isDisconnected() {
		return status == ConnectionStatus.Disconnected;
	}
	
	@Override
	public ServerLogin getLogin() {
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
	@Override
	public IChatroom getChatroom(int id) {
		if (status != ConnectionStatus.Connected)
			return null;
		
		if (chatrooms == null) {
			Log.e(THIS, "Chatrooms empty / status: "+ status);
			return null;
		}
		
		for (IChatroom r: chatrooms) {
			if (r.getID() == id)
			{
				return r;
			}
		}
		
		return null;
	}
	
	@Override
	public IMap getMap() {
		return map;
	}

	@Override
	public String getAvatarURL(int groupID) {
		return currentLogin.server + Paths.getAvatar(groupID);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void executorResult(RequestExecutor<?, Tasks> r, Tasks callbackId) {
		switch (callbackId) {
		case CONFIG:
			serverConfigResult((RequestExecutor<ServerConfig, ?>) r);
			break;
		case LOGOUT:
			logoutResult((RequestExecutor<String, Tasks>) r);
			break;
		case GROUP_LIST:
			availableGroupsResult((RequestExecutor<List<Group>, ?>) r);
			break;
		case AVAILABLE_CHATROOMS:
			availableChatroomsResult((RequestExecutor<List<Chatroom>, ?>) r);
			break;
		case LOGIN:
			loginResult((LoginExecutor<?>) r);
			break;
		case SERVER_INFO:
			serverInfoResult((RequestExecutor<ServerInfo, ?>) r);
			break;
		default:
			Log.e(THIS, "Unknown Executor Callback");
			break;

		}
	}
	
	private Saver save() {
		return new Saver();
	}
	
	private class Saver {
		private Editor edit;
		
		public Saver() {
			this.edit = pref.edit();
		}
		
		public Saver saveLogin() {
			edit.putString(Std.SERVER, currentLogin.server);
			edit.putInt(Std.GROUP_ID, currentLogin.groupID);
			edit.putString(Std.GROUP+Std.PASSWORD, currentLogin.groupPassword);
			edit.putLong(Std.LAST_LOGIN, currentLogin.getLastValidated());
			edit.putString(Std.USER+Std.NAME, currentLogin.name);
			return this;
		}
		
		public Saver saveAuth() {
			edit.putInt(Std.USER_ID, rallyeAuth.userID);
			edit.putString(Std.USER+Std.PASSWORD, rallyeAuth.password);
			
			return this;
		}
		
		public Saver saveChatrooms() {
			Chatroom.saveChatrooms(Model.this, chatrooms);
			
			return this;
		}
		
		public Saver saveConnectionStatus(ConnectionStatus conn) {
			edit.putBoolean(Std.CONNECTED, conn == ConnectionStatus.Connected);
			
			return this;
		}
		
		public Saver saveServerConfig() {
			edit.putLong(Std.LATITUDE, (long) (serverConfig.location.latitude* 1000000));
			edit.putLong(Std.LONGITUDE, (long) (serverConfig.location.longitude* 1000000));
			edit.putString(Std.SERVER+Std.NAME, serverConfig.name);
			edit.putInt(Std.ROUNDS, serverConfig.rounds);
			edit.putInt(Std.ROUND_TIME, serverConfig.roundTime);
			edit.putLong(Std.START_TIME, serverConfig.startTime);
			return this;
		}
		
		public void commit() {
			edit.commit();
		}
		
	}
	
	private void restoreServerConfig() {
		ServerConfig s = new ServerConfig(
				pref.getString(Std.SERVER+Std.NAME, null),
				pref.getLong(Std.LATITUDE, 0)/1000000f,
				pref.getLong(Std.LONGITUDE, 0)/1000000f,
				pref.getInt(Std.ROUNDS, 0),
				pref.getInt(Std.ROUND_TIME, 0),
				pref.getLong(Std.START_TIME, 0));
		
		if (s.isComplete()) {
			serverConfig = s;
			Log.i(THIS, "Server Config recovered");
		} else {
			serverConfig = null;
			Log.e(THIS, "Server Config corrupted");
		}
	}
	
	private void restoreLogin() {
		ServerLogin l = new ServerLogin(pref.getString(Std.SERVER, null),
				pref.getInt(Std.GROUP_ID, 0),
				pref.getString(Std.USER+Std.NAME, null),
				pref.getString(Std.GROUP+Std.PASSWORD, null),
				pref.getLong(Std.LAST_LOGIN, 0));
		
		
		RallyeAuth a = new RallyeAuth(pref.getInt(Std.USER_ID, 0),
									pref.getString(Std.USER+Std.PASSWORD, null),
									l);
		
		if (l.isComplete()) {
			currentLogin = l;
			Log.i(THIS, "Login recovered");
			if (a.password != null) {
				rallyeAuth = a;
				Log.i(THIS, "Auth recovered");
			}
		} else {
			currentLogin = null;
			Log.e(THIS, "Login corrupted");
		}
	}
	
	private void restoreConnectionStatus() {
		if (pref.getBoolean(Std.CONNECTED, false)) {
			status = ConnectionStatus.Connected;
			Log.i(THIS, "Status: Connected recovered");
		} else {
			status = ConnectionStatus.Disconnected;
			Log.i(THIS, "Status: Disconnected recovered");
		}
	}
	
	private ServerLogin getDefaultLogin() {
		return new ServerLogin(Std.DefaultLogin.SERVER,
				Std.DefaultLogin.GROUP,
				Std.DefaultLogin.NAME,
				Std.DefaultLogin.PASSWORD);
	}
	
	@Override
	public GroupUser getUser() {
		return new GroupUser(rallyeAuth.userID, currentLogin.groupID);
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
		Log.i(THIS, "Destroying Model...");
		
		for (Chatroom c: chatrooms) {
			c.onDestroy();
		}
		
		exec.shutdownNow();
		
		db.close();
	}
	
	@Override
	public void addListener(IConnectionStatusListener l) {
		connectionListeners.add(l);
	}
	
	@Override
	public void removeListener(IConnectionStatusListener l) {
		connectionListeners.remove(l);
	}
	
	private void setConnectionStatus(final ConnectionStatus newState) {
		if (newState == status) {
			Log.w(THIS, "Duplicate Status: "+ newState);
			return;
		}
		
		status = newState;
		
		Log.i(THIS, "Status: "+ newState);
		
		uiHandler.post(new Runnable(){
			@Override
			public void run() {
				for(IConnectionStatusListener l: connectionListeners) {
					l.onConnectionStatusChange(newState);
				}
			}
		});
	}
	
	private void connectionFailure(final Exception e, final ConnectionStatus fallbackState) {
		status = fallbackState;
		
		err.connectionFailure(e, fallbackState);
		
		uiHandler.post(new Runnable(){
			@Override
			public void run() {
				for(IConnectionStatusListener l: connectionListeners) {
					l.onConnectionFailed(e, fallbackState);
				}
			}
		});
	}
	
	public ConnectionStatus getConnectionStatus() {
		return status;
	}
}
