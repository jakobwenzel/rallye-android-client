package de.stadtrallye.rallyesoft.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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

import de.rallye.model.structures.PictureSize;
import de.rallye.model.structures.UserAuth;
import de.stadtrallye.rallyesoft.model.structures.Group;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.structures.GroupUser;
import de.stadtrallye.rallyesoft.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.model.structures.ServerConfig;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.exceptions.LoginFailedException;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.JSONObjectRequestExecutor;
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

	
	private SharedPreferences pref;
	final private Context context;
	
	private ServerLogin currentLogin;
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
	 * If possible restore a Model from RAM or Settings
	 * @param context needed for Database, Preferences, GCMid => give ApplicationContext to avoid leaking contexts during configuration changes
	 * @return existing Model or one restored from settings
	 */
	public static IModel getModel(Context context) {
		if (model != null) {
			return model.reuse();
		} else {
			return model = new Model(context, getDefaultPreferences(context));
		}
	}

	public static IModel createEmptyModel(Context context) {
		return new Model(context, null);
	}

	public static void switchToNew(IModel newModel) {
		if (!(newModel instanceof Model))
			return;

		model.logout();
		model.onDestroy();

		Model.model = (Model) newModel;

		model.saveModel();
	}

	@Override
	public boolean isEmpty() {
		return !currentLogin.hasServer();
	}
	
	private static SharedPreferences getDefaultPreferences(Context context) {
		return context.getSharedPreferences(Std.CONFIG_MAIN, Context.MODE_PRIVATE);
	}

	/**
	 * Ensure that this Model has a working DB Connection and Executor (if for some reason the model was destroyed, but the context (and static model with it) survived)
	 * @return for convenience
	 */
	private Model reuse() {
		if (!model.db.isOpen()) {
			Log.w(THIS, "Resusing old Model: reopening DB");
			initDatabase();
		}
		if (model.exec.isShutdown()) {
			Log.w(THIS, "Resusing old Model: restarting Executor");
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

		initExecutor();

		try {

			if (pref != null) {
				restoreLogin();
				restoreServerConfig();
				restoreConnectionStatus();
			} else {
				currentLogin = new ServerLogin();
				setConnectionStatus(ConnectionStatus.Disconnected);
			}

		} catch (Exception e) {
			Log.w(THIS, "No usable Login-Data found -> stay offline", e);
			setConnectionStatus(ConnectionStatus.Disconnected);
		}

		String uniqueID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		factory = new RequestFactory(currentLogin, uniqueID);
		factory.setPushID(GCMRegistrar.getRegistrationId(context));

		if (isConnected()) {
			try {
				chatrooms = Chatroom.getChatrooms(this);
			} catch (Exception e) {
				Log.e(THIS, "Chatrooms could not be restored");
				throw e;
			}
		}

		map = new Map(this);

        checkConfiguration();
	}
	
	@Override
	public void logout() {
		if (currentLogin == null || !isConnected()) {
			Exception e = err.logoutInvalid();
			return;
		}
		
		setConnectionStatus(ConnectionStatus.Disconnecting);
		if (pref != null)
			save().saveConnectionStatus().commit();
		
		try {
			exec.execute(new RequestExecutor<String, Tasks>(factory.logoutRequest(), null, this, Tasks.LOGOUT));
		} catch (Exception e) {
			err.requestException(e);
		}
	}
	
	private void logoutResult(RequestExecutor<String, ?> r) {
		currentLogin.setUserAuth(null);
		setConnectionStatus(ConnectionStatus.Disconnected);
	}

	@Override
	public String setServer(String server) throws MalformedURLException {
		if (!server.endsWith("/"))
			server = server +"/";

		this.currentLogin.setServer(new URL(server));
		return server;
	}
	
	@Override
	public void login(String username, int groupID, String groupPassword) {
		synchronized (this) {
			if (!isDisconnected()) {
				if (isConnected()) {
					err.loggedIn();
				} else if (isConnectionChanging()) {
					err.concurrentConnectionChange("Login");
				}
				return;
			}//Sanity
			
			setConnectionStatus(ConnectionStatus.Connecting);
			currentLogin.setName(username);
			currentLogin.setGroupID(groupID);
			currentLogin.setGroupPassword(groupPassword);
		}
		
		try {
			factory.setPushID(GCMRegistrar.getRegistrationId(context));//TODO: necessary?
			
			exec.execute(new JSONObjectRequestExecutor<>(factory.loginRequest(), new ServerLogin.AuthConverter(), this, Tasks.LOGIN));
		
		} catch (Exception e) {
			err.requestException(e);
			connectionFailure(e, ConnectionStatus.Disconnected);
		}
		
	}
	
	public void loginResult(RequestExecutor<UserAuth, ?> r) {
		if (r.isSuccessful()) {
			
			currentLogin.setUserAuth(r.getResult());
			
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
                }

				checkConnectionComplete();
		} else {
			err.asyncTaskResponseError(r.getException());
		}
	}
	
	private void checkConnectionComplete() {
		if (status == ConnectionStatus.Connecting && chatrooms != null && chatrooms.size() > 0 && serverConfig != null) {
			setConnectionStatus(ConnectionStatus.Connected);
		}
			
	}

	@Override
	public void getAvailableGroups(IAvailableGroupsCallback callback) {
		if (!currentLogin.hasServer()) {
			err.serverNotSet();
			callback.availableGroups(null);
			return;
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
	public void getServerInfo(IServerInfoCallback callback) {
		if (!currentLogin.hasServer()) {
			err.serverNotSet();
			callback.serverInfo(null);
			return;
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

	@Override
	public void saveModel() {
		if (pref == null)
			pref = getDefaultPreferences(context);

		save().saveLogin().saveServerConfig().saveChatrooms().saveConnectionStatus().commit();
	}

	@Override
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
		return currentLogin;
	}

	@Override
	public GroupUser getUser() {
		return new GroupUser(currentLogin.getUserID(), currentLogin.getGroupID());
	}

	/**
	 * Get the chatrooms available to the current user
	 * @return Unmodifiable List
	 */
	@Override
	public List<? extends IChatroom> getChatrooms() {
		return Collections.unmodifiableList(chatrooms);
	}


	/**
	 *
	 * @param id the official server side chatroomID
	 * @return Model of that specific chatroom / null if it does not exist, user has no access
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
		return currentLogin.getServer().toString() + Paths.getAvatar(groupID);
	}

	@Override
	public String getServerPictureURL() {
		return currentLogin.getServer().toString() + Paths.SERVER_PICTURE;
	}

	@Override
	public String getUrlFromImageId(int pictureID, PictureSize size) {
		return currentLogin.getServer().toString() + Paths.getPic(pictureID, size);
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
			loginResult((RequestExecutor<UserAuth, ?>) r);
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
			edit.putString(Std.SERVER, currentLogin.getServer().toString());
			edit.putInt(Std.GROUP_ID, currentLogin.getGroupID());
			edit.putString(Std.GROUP+Std.PASSWORD, currentLogin.getGroupPassword());
			edit.putLong(Std.LAST_LOGIN, currentLogin.getLastValidated());
			edit.putString(Std.USER+Std.NAME, currentLogin.getName());
			edit.putInt(Std.USER_ID, currentLogin.getUserID());
			edit.putString(Std.USER+Std.PASSWORD, currentLogin.getUserPassword());
			return this;
		}
		
		public Saver saveChatrooms() {
			Chatroom.saveChatrooms(Model.this, chatrooms);
			
			return this;
		}
		
		public Saver saveConnectionStatus() {
			edit.putBoolean(Std.CONNECTED, status == ConnectionStatus.Connected);
			
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
		UserAuth auth = new UserAuth(pref.getInt(Std.USER_ID, 0), pref.getString(Std.USER+Std.PASSWORD, null));
		if (auth.password == null)
			auth = null;

		try {
			currentLogin = new ServerLogin(pref.getString(Std.SERVER, null),
					pref.getInt(Std.GROUP_ID, 0),
					pref.getString(Std.USER+Std.NAME, null),
					pref.getString(Std.GROUP+Std.PASSWORD, null),
					pref.getLong(Std.LAST_LOGIN, 0),
					auth);
			Log.i(THIS, "Login recovered");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Log.i(THIS, "Login corrupted");
			currentLogin = new ServerLogin();
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

	
	/**
	 * shutdown Executor (prevent it from accepting new Tasks but complete all previously accepted ones)
	 */
	@Override
	public void onDestroy() {
		Log.i(THIS, "Destroying Model...");
		
		db.close();

		exec.shutdown();
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
