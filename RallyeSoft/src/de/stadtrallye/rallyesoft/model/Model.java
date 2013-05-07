package de.stadtrallye.rallyesoft.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.maps.model.LatLng;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.exceptions.LoginFailedException;
import de.stadtrallye.rallyesoft.model.backend.DatabaseOpenHelper;
import de.stadtrallye.rallyesoft.model.backend.DatabaseOpenHelper.Groups;
import de.stadtrallye.rallyesoft.model.comm.RequestFactory;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.LoginExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.model.structures.Login;
import de.stadtrallye.rallyesoft.model.structures.MapNode;
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
	
	
	enum Tasks { LOGIN, CHECK_SERVER, MAP_NODES, LOGOUT, CONFIG };
	
	
	private static Model model; // Singleton Pattern

	
	final private SharedPreferences pref;
	final private Context context;
	
	private Login currentLogin;
	private ServerConfig serverConfig;
	
	private ConnectionStatus status = ConnectionStatus.Unknown;
	final ArrayList<IConnectionStatusListener> connectionListeners  = new ArrayList<IConnectionStatusListener>();
	final ArrayList<IMapListener> mapListeners = new ArrayList<IMapListener>();
	
	final Handler uiHandler = new Handler(Looper.getMainLooper());
	final RequestFactory factory;
	final ExecutorService exec;
	
	private List<Chatroom> chatrooms;
	
	private SQLiteDatabase db;
	
	/**
	 * Model can now be kept through Configuration Changes
	 * @param context needed for Database, Preferences, GCMid (and Res-Strings for default_login [not lang-specific]) => give ApplicationContext
	 * @param loggedIn login state to assume until checked
	 * @return
	 */
	public static Model getInstance(Context context, boolean loggedIn) {
		if (model != null)
			return model.ensureConnections();
		else
			return model = new Model(context, getDefaultPreferences(context), loggedIn);
	}
	
	/**
	 * Make sure we have a valid DatabaseConnection
	 * @return Model instance for convenience
	 */
	private Model ensureConnections() {
		if (db == null)
			db = new DatabaseOpenHelper(context).getWritableDatabase();
		return model;
	}
	
	private static SharedPreferences getDefaultPreferences(Context context) {
		return context.getSharedPreferences(context.getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
	}
	
	private Model(Context context, SharedPreferences pref, boolean loggedIn) {
		this.pref = pref;
		this.context = context;
//		this.connectionStatus = loggedIn? ConnectionStatus.Connected : ConnectionStatus.Unknown;//TODO: Network check
		
		SQLiteOpenHelper helper = new DatabaseOpenHelper(context);
		db = helper.getWritableDatabase();
		
		exec = Executors.newCachedThreadPool();
		
		currentLogin = Login.fromPref(pref);
		
		factory = new RequestFactory(null, Std.GCM);
		
		try {
			if (currentLogin == null || !currentLogin.isComplete() || !isDbSynchronized())
				throw new LoginFailedException();
				
			URL url = new URL(currentLogin.getServer());
			factory.setBaseURL(url);
			factory.setID(GCMRegistrar.getRegistrationId(context));
			
			Log.i(THIS, "Cold Start: Found Login-Data and DB -> checking connection");
			
			checkConnectionStatus();
		} catch (Exception e) {
			Log.w(THIS, "No usable Login-Data found -> stay offline");
			setConnectionStatus(ConnectionStatus.Disconnected);
		}
	}
	
	/**
	 * Checks if DB contains at least one group
	 * (Means we have been logged in before)
	 * @return
	 */
	private boolean isDbSynchronized() {//TODO: precise check
		Cursor c = db.query(Groups.TABLE, new String[]{Groups.KEY_NAME}, Groups.KEY_ID+"="+currentLogin.getGroup(), null, null, null, null);
		if (c.getCount() != 1)
			return false;
		else
			return true;
	}
	
	SQLiteDatabase getDb() {
		return db;
	}
	
	
	@Override
	public void logout() {
		if (currentLogin == null || status == ConnectionStatus.Disconnected) {
			Exception e = err.logoutInvalid();
			connectionFailure(e, ConnectionStatus.Disconnected);
			return;
		}
		
		try {
			setConnectionStatus(ConnectionStatus.Disconnecting);
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
		if (isLoggedIn()) {
			err.loggedIn();
			return;
		} else if (isConnectionChanging()) {
			err.concurrentConnectionChange("Login");
		} else if (!login.isComplete()) {
			err.loginInvalid(login);
			return;
		} else if (login.equals(currentLogin)) {
			login = currentLogin;
		}// Sanity
		
		try {
			factory.setID(GCMRegistrar.getRegistrationId(context));
			factory.setBaseURL(new URL(login.getServer()));
			
			LoginExecutor rex = new LoginExecutor(factory.loginRequest(login), factory.logoutRequest(), login, this);
			setConnectionStatus(ConnectionStatus.Connecting);
			exec.execute(rex);
		
		} catch (Exception e) {
			err.requestException(e);
		}
	}
	
	@Override
	public void loginResult(LoginExecutor r) {
		if (r.isSuccessful()) {
			chatrooms = r.getResult();
			
			currentLogin = r.getLogin();
			writeLoginAndChatrooms();//TODO: setLogin();
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
	public void checkConnectionStatus() {
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
			setConnectionStatus((r.getResponseCode() >= 200 && r.getResponseCode() < 300)? ConnectionStatus.Connected : ConnectionStatus.Disconnected);
		} else
			setConnectionStatus(ConnectionStatus.Disconnected);
	}
	
	//TODO: always precache / refresh on mapNeeded??
	public void getMapNodes() {
		if (!isLoggedIn()) {
			err.notLoggedIn();
			return;
		}
		try {
			exec.execute(new JSONArrayRequestExecutor<MapNode, Tasks>(factory.mapNodesRequest(), new MapNode.NodeConverter(), this, Tasks.MAP_NODES));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void mapNodeResult(JSONArrayRequestExecutor<MapNode, ?> r) {
		if (r.isSuccessful()) {
			try {
				notifyMapUpdate(r.getResult());
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
		} else
			err.asyncTaskResponseError(r.getException());
	}
	
	private void refreshServerConfig() {
		try {
			Log.d(THIS, "getting Server config");
			exec.execute(new RequestExecutor<ServerConfig, Tasks>(factory.serverConfigRequest(), new ServerConfig.ServerConfigConverter(), this, Tasks.CONFIG));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void serverConfigResult(RequestExecutor<ServerConfig, ?> r) {//TODO synchronize
		if (r.isSuccessful()) {
			try {
				serverConfig = r.getResult();
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
		}
	}
	
	public boolean isConnectionChanging() {
		return status == ConnectionStatus.Connecting || status == ConnectionStatus.Disconnecting;
	}
	
	@Override
	public boolean isLoggedIn() {
		return status == ConnectionStatus.Connected;
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
	
	public LatLng getMapLocation() {
		return serverConfig.getLocation();
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
		case MAP_NODES:
			mapNodeResult((JSONArrayRequestExecutor<MapNode, ?>) r);
			break;
		default:
			Log.e(THIS, "Unknown Executor Callback");
			break;
		
		}
	}

	private void writeLoginAndChatrooms() {
		SharedPreferences.Editor edit = pref.edit();
		currentLogin.toPref(edit);
		
		ContentValues insert = new ContentValues();
		insert.put(Groups.KEY_ID, currentLogin.getGroup());
		//TODO: put Name
		db.insert(Groups.TABLE, null, insert);
		
		for (Chatroom c: chatrooms) {
			c.writeToDb();
		}
		
		edit.commit();
	}
	
	private Login getDefaultLogin() {
		return new Login(context.getString(R.string.default_server),
				Integer.valueOf(context.getString(R.string.default_group)),
				context.getString(R.string.default_name),
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
		Log.i(THIS, "Destroying Model...");
		
		for (Chatroom c: chatrooms) {
			c.onDestroy();
		}
		
		exec.shutdownNow();
		
		db.close();
	}
	
	@Override
	public void addListener(IMapListener l) {
		mapListeners.add(l);
	}
	
	@Override
	public void removeListener(IMapListener l) {
		mapListeners.remove(l);
	}

	private void notifyMapUpdate(final List<MapNode> list) {
		uiHandler.post(new Runnable(){
			@Override
			public void run() {
				for(IMapListener l: mapListeners) {
					l.nodeUpdate(list);
				}
			}
		});
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
