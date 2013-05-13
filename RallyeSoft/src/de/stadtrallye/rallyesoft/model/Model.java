package de.stadtrallye.rallyesoft.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.exceptions.LoginFailedException;
import de.stadtrallye.rallyesoft.model.comm.RequestFactory;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Groups;
import de.stadtrallye.rallyesoft.model.executors.LoginExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.model.structures.Login;
import de.stadtrallye.rallyesoft.model.structures.ServerConfig;

/**
 * My Model
 * Should be the only Class to write to Preferences
 * All Tasks should start here
 * @author Ray
 *
 */
public class Model extends Binder implements IModel, LoginExecutor.Callback, RequestExecutor.Callback<Model.Tasks> {
	
	private static final String THIS = Model.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);
	@SuppressWarnings("unused")
	final private static boolean DEBUG = false;
	
	
	enum Tasks { LOGIN, CHECK_SERVER, MAP_NODES, LOGOUT, CONFIG, GROUP_LIST };
	
	
	private static Model model; // Singleton Pattern

	
	final private SharedPreferences pref;
	final private Context context;
	
	private Login currentLogin;
	ServerConfig serverConfig;
	
	private ConnectionStatus status = ConnectionStatus.Unknown; //TODO: add network state
	final ArrayList<IConnectionStatusListener> connectionListeners  = new ArrayList<IConnectionStatusListener>();
	
	final Handler uiHandler = new Handler(Looper.getMainLooper());
	final RequestFactory factory;
	ExecutorService exec;
	
	private List<Chatroom> chatrooms;
	final private Map map;
	
	SQLiteDatabase db;
	
	/**
	 * Model can now be kept through Configuration Changes
	 * @param context needed for Database, Preferences, GCMid (and Res-Strings for default_login [not lang-specific]) => give ApplicationContext
	 * @param loggedIn login state to assume until checked
	 * @return
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
		SQLiteOpenHelper helper = new DatabaseHelper(context);
		db = helper.getWritableDatabase();
	}
	
	private Model(Context context, SharedPreferences pref) {
		this.pref = pref;
		this.context = context;
		
		initDatabase();
		
		initExecutor();
		
		restoreLogin();
		
		factory = new RequestFactory(null, Std.GCM);
		
		try {
			if (currentLogin == null || !currentLogin.isComplete() || !isDbSynchronized())
				throw new LoginFailedException();
				
			URL url = new URL(currentLogin.server);
			factory.setBaseURL(url);
			factory.setDeviceID(GCMRegistrar.getRegistrationId(context));
			
			
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
			if (status == ConnectionStatus.Unknown) {
				Log.e(THIS, "ConnectionStatus previously not connected");
				checkLoginStatus();
			}
		} catch (Exception e) {
			Log.w(THIS, "No usable Login-Data found -> stay offline");
			setConnectionStatus(ConnectionStatus.Disconnected);
		}
		
		map = new Map(this);
	}
	
	/**
	 * Checks if DB contains at least one group
	 * (Means we have been logged in before)
	 * @return
	 */
	private boolean isDbSynchronized() {//TODO: precise check
		Cursor c = db.query(Groups.TABLE, new String[]{Groups.KEY_NAME}, Groups.KEY_ID+"="+currentLogin.group, null, null, null, null);
		if (c.getCount() != 1)
			return false;
		else
			return true;
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
		setConnectionStatus(ConnectionStatus.Disconnected);
	}
	
	@Override
	public void login(Login login) {
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
				factory.setDeviceID(GCMRegistrar.getRegistrationId(context));
				factory.setBaseURL(new URL(login.server));
				
				LoginExecutor rex = new LoginExecutor(factory.loginRequest(login), factory.logoutRequest(), login, this);
				exec.execute(rex);
			
			} catch (Exception e) {
				err.requestException(e);
				connectionFailure(e, ConnectionStatus.Disconnected);
			}
		
	}
	
	@Override
	public void loginResult(LoginExecutor r) {
		if (r.isSuccessful()) {
			chatrooms = r.getResult();
			
			currentLogin = r.getLogin();
			
			save().saveChatrooms().saveLogin().saveConnectionStatus(ConnectionStatus.Connected).commit();
			
			refreshServerConfig();
			setConnectionStatus(ConnectionStatus.Connected);
		} else {
			connectionFailure(r.getException(), ConnectionStatus.Disconnected);
		}
	}
	
	/**
	 * Start async check if the server accepts us
	 */
	@Override
	public void checkLoginStatus() {
		if (isConnectionChanging()) {
			err.concurrentConnectionChange("Status");
			return;
		}
		
		setConnectionStatus(ConnectionStatus.Connecting);
		
		try {
			exec.execute(new RequestExecutor<String, Tasks>(factory.serverStatusRequest(), null, this, Tasks.CHECK_SERVER));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void checkStatusResult(RequestExecutor<String, ?> r) {
		if (r.isSuccessful()) {
			currentLogin.validated();
			chatrooms = Chatroom.getChatrooms(this);
			refreshServerConfig();
			ConnectionStatus state = (r.getResponseCode() >= 200 && r.getResponseCode() < 300)? ConnectionStatus.Connected : ConnectionStatus.Disconnected;
			save().saveConnectionStatus(state).commit();
			setConnectionStatus(state);
		} else
			setConnectionStatus(ConnectionStatus.Disconnected);
	}
	
	private void refreshServerConfig() {
		try {
			Log.d(THIS, "getting Server config");
			exec.execute(new RequestExecutor<ServerConfig, Tasks>(factory.serverConfigRequest(), new ServerConfig.ServerConfigConverter(), this, Tasks.CONFIG));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void serverConfigResult(RequestExecutor<ServerConfig, ?> r) {
		if (r.isSuccessful()) {
			try {
				serverConfig = r.getResult();
				save().saveServerConfig().commit();
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
		}
	}
	
	public void groupList() {
		if (!isConnected()) {
			err.notLoggedIn();
			return;
		}
		
		try {
//			factory.setAuth(currentLogin);
			
			Log.d(THIS, "testing auth");
			exec.execute(new RequestExecutor<String, Tasks>(factory.groupListRequest(),null, this, Tasks.GROUP_LIST));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void groupListResult(RequestExecutor<String, ?> r) {
		Log.i(THIS, r.getResult());
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
	
	@SuppressWarnings("unchecked")
	@Override
	public void executorResult(RequestExecutor<?, Tasks> r, Tasks callbackId) {
		switch (callbackId) {
		case CHECK_SERVER:
			checkStatusResult((RequestExecutor<String, Tasks>) r);
			break;
		case CONFIG:
			serverConfigResult((RequestExecutor<ServerConfig, ?>) r);
			break;
		case LOGOUT:
			logoutResult((RequestExecutor<String, Tasks>) r);
			break;
		case GROUP_LIST:
			groupListResult((RequestExecutor<String, ?>) r);
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
			edit.putInt(Std.GROUP, currentLogin.group);
			edit.putString(Std.PASSWORD, currentLogin.password);
			edit.putLong(Std.LAST_LOGIN, currentLogin.getLastValidated());
			edit.putString(Std.USER+Std.NAME, currentLogin.name);
			edit.putInt(Std.USER_ID, currentLogin.getId());
			return this;
		}
		
		public Saver saveChatrooms() {
			ContentValues insert = new ContentValues();
			insert.put(Groups.KEY_ID, currentLogin.group);
			//TODO: put Name [need to get name]
			db.insert(Groups.TABLE, null, insert);
			
			for (Chatroom c: chatrooms) {
				c.writeToDb();
			}
			
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
			edit.putInt(Std.START_TIME, serverConfig.startTime);
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
				pref.getInt(Std.START_TIME, 0));
		
		if (s.isComplete()) {
			serverConfig = s;
			Log.i(THIS, "Server Config recovered");
		} else {
			serverConfig = null;
			Log.e(THIS, "Server Config corrupted");
		}
	}
	
	private void restoreLogin() {
		Login l = new Login(pref.getString(Std.SERVER, null),
				pref.getInt(Std.GROUP, 0),
				pref.getString(Std.USER+Std.NAME, null),
				pref.getString(Std.PASSWORD, null),
				pref.getInt(Std.USER_ID, Login.NO_ID),
				pref.getLong(Std.LAST_LOGIN, 0));
		
		if (l.isComplete()) {
			currentLogin = l;
			Log.i(THIS, "Login recovered");
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
			status = ConnectionStatus.Unknown;
			Log.i(THIS, "Status: Unkown recovered");
		}
	}
	
	private Login getDefaultLogin() {
		return new Login(Std.DefaultLogin.SERVER,
				Std.DefaultLogin.GROUP,
				Std.DefaultLogin.NAME,
				Std.DefaultLogin.PASSWORD);
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
