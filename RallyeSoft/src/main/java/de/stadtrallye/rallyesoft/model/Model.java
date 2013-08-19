package de.stadtrallye.rallyesoft.model;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.GroupUser;
import de.rallye.model.structures.PictureSize;
import de.rallye.model.structures.ServerConfig;
import de.rallye.model.structures.ServerInfo;
import de.rallye.model.structures.UserAuth;
import de.stadtrallye.rallyesoft.model.converters.JsonConverters;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Groups;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Users;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.JSONObjectRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.net.Paths;
import de.stadtrallye.rallyesoft.net.RequestFactory;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;

import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.EDIT_CHATROOMS;
import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.EDIT_GROUPS;
import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.EDIT_USERS;

/**
 * My Model
 * Should be the only Class to write to Preferences
 * All CallbackIds should start here
 * @author Ray
 *
 */
public class Model implements IModel, RequestExecutor.Callback<Model.CallbackIds> {

	// static
	private static final String THIS = Model.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);
	@SuppressWarnings("unused")
	final private static boolean DEBUG = false;

	enum CallbackIds { LOGIN, LOGOUT, CONFIG, GROUP_LIST, SERVER_INFO, USER_LIST, AVAILABLE_CHATROOMS }

	// Singleton Pattern
	private static Model model;

	// Android / UI Specific
	private SharedPreferences pref;
	final Context context;
	final Handler uiHandler = new Handler(Looper.getMainLooper());
	final NotificationManager notificationService;

	// State
	private ConnectionState state;
//	private int state;
	private ServerLogin currentLogin;
	private ServerConfig serverConfig;
	private ServerInfo serverInfo;

	// Sub Modules
	private List<Chatroom> chatrooms;
	private final Map map;
	private final Tasks tasks;

	// Listener
	private final ArrayList<IModelListener> modelListeners = new ArrayList<>();
	
	// Temp
	private List<Group> groups;// Needs to be stored here until we have preferences and can save it to Database
	
	// Helper
	final RequestFactory factory;
	ExecutorService exec;
	SQLiteDatabase db;
	int deprecatedTables;
	
	/**
	 * If possible restore a Model from RAM or Settings
	 * @param context needed for Database, Preferences, GCMid => give ApplicationContext to avoid leaking contexts during configuration changes
	 * @return existing Model or one restored from settings
	 */
	public static IModel getInstance(Context context) {
		if (model != null) {
			return model.reuse();
		} else {
			return model = new Model(context, getDefaultPreferences(context));
		}
	}

	/**
	 * Create a new Model, separate from the general Singleton Pattern of Model
	 * @param context needed for Database, Preferences etc
	 * @return a new completely empty Model, ready to connect to a new server
	 */
	public static IModel createEmptyModel(Context context) {
		return new Model(context, null);
	}

	/**
	 * @param modelActivity the parent Activity who must initialize the Model
	 * @return the current Model
	 */
	public static IModel getModel(Activity modelActivity) {
		try {
			return ((IModelActivity) modelActivity).getModel();
		} catch (ClassCastException e) {
			Log.e(THIS, "The Activity "+ modelActivity +" must implement IModelActivity", e);
			throw new IllegalArgumentException("The Activity must implement IModelActivity");
		}
	}

	/**
	 * Switch the new Model from {@link #createEmptyModel(android.content.Context)} in to the Singleton Pattern of Model after it has been tested
	 * Will kill and terminate the old Model immediately
	 * @param newModel a Model whose connection to a new server was successful
	 */
	private static void switchToNew(Model newModel) {
		if (model != null) {
			if (!model.currentLogin.getServer().equals(newModel.currentLogin.getServer()))
				model.logout();

			model.destroy();
		}

		Model.model = (Model) newModel;

		model.saveModel();
	}

	@Override
	public void acceptModel() {
		Model.switchToNew(this);
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
		if (db == null) {
			Log.w(THIS, "Reusing old Model: reopening DB");
			initDatabase();
		}
		if (exec == null) {
			Log.w(THIS, "Reusing old Model: restarting Executor");
			initExecutor();
		}
		
		return this;
	}
	
	private void initExecutor() {
		exec = Executors.newCachedThreadPool();
	}
	
	private void initDatabase() {
		DatabaseHelper helper = new DatabaseHelper(context);
		db = helper.getWritableDatabase();

		deprecatedTables = helper.getEditedTables();
	}
	
	private Model(Context context, SharedPreferences pref) {
		this.pref = pref;
		this.context = context;

		notificationService = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		initDatabase();

		initExecutor();

		if (pref != null) {
			try {
				restoreLogin();
				restoreServerConfig();
				restoreServerInfo();
				restoreConnectionStatus();
			} catch (Exception e) {
				Log.e(THIS, "Corrupted Login-Data: staying offline, Please send me a Bug-Report!!", e);
				if (currentLogin == null)
					currentLogin = new ServerLogin();
				setConnectionState(ConnectionState.None);
			}
		} else {
			currentLogin = new ServerLogin();
			setConnectionState(ConnectionState.None);
		}

		String uniqueID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		factory = new RequestFactory(currentLogin, uniqueID);
		factory.setPushID(GCMRegistrar.getRegistrationId(context));

		if (state == ConnectionState.Connected) {
			if ((deprecatedTables & EDIT_CHATROOMS) > 0) {
				refreshAvailableChatrooms();
				deprecatedTables &= ~EDIT_CHATROOMS;
			} else {
				try {
					chatrooms = Chatroom.getChatrooms(this);
				} catch (Exception e) {
					Log.e(THIS, "Chatrooms could not be restored");
				}
				refreshAvailableChatrooms();
			}
			if ((deprecatedTables & EDIT_USERS) > 0) {
				refreshAllUsers();
				deprecatedTables &= ~EDIT_USERS;
			}
			if ((deprecatedTables & EDIT_GROUPS) > 0) {
				refreshAvailableGroups();
				deprecatedTables &= ~EDIT_GROUPS;
			}
			refreshConfiguration();
		}

		map = new Map(this);
		tasks = new Tasks(this);
	}
	
	@Override
	public synchronized void logout() {
		if (currentLogin == null || state != ConnectionState.Connected) {
			Log.w(THIS, "Cannot logout: Not logged in");
			return;
		}
		
		setConnectionState(ConnectionState.Disconnecting);
		if (pref != null)
			save().saveConnectionStatus().commit();
		
		try {
			exec.execute(new RequestExecutor<String, CallbackIds>(factory.logoutRequest(), null, this, CallbackIds.LOGOUT));
		} catch (Exception e) {
			err.requestException(e);
		}
	}
	
	@SuppressWarnings("UnusedParameters")
	private void logoutResult(RequestExecutor<String, ?> r) {
		currentLogin.setUserAuth(null);
		setConnectionState(ConnectionState.Disconnected);
	}

	@Override
	public synchronized String setServer(String server) throws MalformedURLException {
		if (!server.endsWith("/"))
			server = server +"/";

		if (state != ConnectionState.None)
			throw new IllegalStateException("This Model already has a Server");

		this.currentLogin.setServer(new URL(server));

//		setConnectionState(ConnectionState.Disconnected);

		refreshServerInfo();
		refreshAvailableGroups();
		return server;
	}

	private synchronized void login() {
		setConnectionState(ConnectionState.Connecting);

		try {
			factory.setPushID(GCMRegistrar.getRegistrationId(context));
			exec.execute(new JSONObjectRequestExecutor<>(factory.loginRequest(), new ServerLogin.AuthConverter(), this, CallbackIds.LOGIN));
		} catch (Exception e) {
			err.requestException(e);
			connectionFailure(e, ConnectionState.Invalid);
		}
	}
	
	@Override
	public synchronized void login(String username, int groupID, String groupPassword) {
		if (state != ConnectionState.None) {
			throw new IllegalStateException("Already connected");
		}

		setConnectionState(ConnectionState.Connecting);
		currentLogin.setName(username);
		currentLogin.setGroupID(groupID);
		currentLogin.setGroupPassword(groupPassword);
		
		try {
			factory.setPushID(GCMRegistrar.getRegistrationId(context));// if newly installed GCM_ID has on occasion not been available at first start
			exec.execute(new JSONObjectRequestExecutor<>(factory.loginRequest(), new ServerLogin.AuthConverter(), this, CallbackIds.LOGIN));
		} catch (Exception e) {
			err.requestException(e);
			connectionFailure(e, ConnectionState.Invalid);
		}
		
	}
	
	public void loginResult(RequestExecutor<UserAuth, ?> r) {
		if (r.isSuccessful()) {
			
			currentLogin.setUserAuth(r.getResult());
			currentLogin.validated();
			setConnectionState(ConnectionState.InternalConnected);
			
			refreshConfiguration();
			map.refresh();
			tasks.refresh();
		} else {
			connectionFailure(r.getException(), ConnectionState.Invalid);
		}
	}

	@Override
	public synchronized void reconnect() {
		if (!canReconnect()) {
			throw new IllegalStateException("Cannot reconnect, not enough information");
		}

		if (currentLogin.hasUserAuth()) {
			setConnectionState(ConnectionState.InternalConnected);
			refreshConfiguration();
		} else {
			login();
		}
	}

	@Override
	public synchronized boolean canReconnect() {
		return (state == ConnectionState.Disconnected || state == ConnectionState.TemporaryNotAvailable) && currentLogin.isValid();
	}

	@Override
	public synchronized void refreshConfiguration() {
		if (!isConnectedInternal()) {
			throw new IllegalStateException("refreshConfiguration needs at least State=InternalConnected");
		}

        refreshAvailableChatrooms();
        refreshServerConfig();
	}
	
	private void refreshServerConfig() {
		try {
			Log.d(THIS, "getting Server config");
			exec.execute(new JSONObjectRequestExecutor<>(factory.serverConfigRequest(), new JsonConverters.ServerConfigConverter(), this, CallbackIds.CONFIG));
		} catch (HttpRequestException e) {
			err.requestException(e);
			connectionFailure(e, ConnectionState.Invalid);
		}
	}
	
	private void serverConfigResult(RequestExecutor<ServerConfig, ?> r) {
		if (r.isSuccessful()) {
				ServerConfig serverConfig = r.getResult();

				if (!serverConfig.equals(this.serverConfig)) {
					this.serverConfig = serverConfig;
					if (pref != null)
						save().saveServerConfig().commit();
					Log.d(THIS, "Server Config has changed, replacing");

					uiHandler.post(new Runnable() {
						@Override
						public void run() {
							for (IModelListener l: modelListeners) {
								l.onServerConfigChange();
							}
						}
					});
				}

				checkConnectionComplete();
		} else {
			Exception e = r.getException();
			err.asyncTaskResponseError(e);
			commError(e);
		}
	}
	
	private void refreshAvailableChatrooms() {
		try {
			Log.d(THIS, "getting available chatrooms");
			exec.execute(new JSONArrayRequestExecutor<>(factory.availableChatroomsRequest(), new JsonConverters.ChatroomConverter(this), this, CallbackIds.AVAILABLE_CHATROOMS));
		} catch (HttpRequestException e) {
			err.requestException(e);
			connectionFailure(e, ConnectionState.Invalid);
		}
	}
	
	private void availableChatroomsResult(RequestExecutor<List<Chatroom>, ?> r) {
		if (r.isSuccessful()) {
				List<Chatroom> chatrooms = r.getResult();

                if (!chatrooms.equals(this.chatrooms)) {
                    this.chatrooms = chatrooms;
					if (pref != null)
						save().saveChatrooms().commit();
					Log.d(THIS, "Chatroom Config has changed, replacing");
                }

				checkConnectionComplete();
		} else {
			Exception e = r.getException();
			err.asyncTaskResponseError(e);
			commError(e);
		}
	}
	
	private synchronized void checkConnectionComplete() {
		if ((state == ConnectionState.Connecting || state == ConnectionState.InternalConnected) && chatrooms != null && chatrooms.size() > 0 && serverConfig != null) {
			setConnectionState(ConnectionState.Connected);

			if (pref != null)
				save().saveConnectionStatus().commit();
		}
	}

	@Override
	public void refreshAvailableGroups() {
		if (!currentLogin.hasServer()) {
			throw new IllegalStateException("Cannot request available groups without setServer() first");
		}
		
		try {
			exec.execute(new JSONArrayRequestExecutor<>(factory.availableGroupsRequest(), new JsonConverters.GroupConverter(), this, CallbackIds.GROUP_LIST));
		} catch (HttpRequestException e) {
			err.requestException(e);
			connectionFailure(e, ConnectionState.Invalid);
		}
	}
	
	private void availableGroupsResult(RequestExecutor<List<Group>, ?> r) {
		if (r.isSuccessful()) {
			groups = r.getResult();

			if (pref != null)
				save().saveGroups().commit();

			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					for (IModelListener l: modelListeners) {
						l.onAvailableGroupsChange(groups);
					}
				}
			});
		}
	}

	private synchronized void refreshAllUsers() {
		if (state != ConnectionState.Connected) {
			throw new IllegalStateException("Need to be connected to a server");
		}

		try {
			exec.execute(new JSONArrayRequestExecutor<>(factory.allUsersRequest(), new JsonConverters.GroupUserConverter(), this, CallbackIds.USER_LIST));
		} catch (HttpRequestException e) {
			err.requestException(e);
			connectionFailure(e, ConnectionState.Invalid);
		}
	}

	private void refreshAllUsersResult(RequestExecutor<List<GroupUser>, ?> r) {
		if (r.isSuccessful()) {
			final List<GroupUser> users = r.getResult();

			save().saveUsers(users).commit();

			for (Chatroom c: chatrooms) {
				c.onDbChange();
			}
		} else {
			Exception e = r.getException();
			err.asyncTaskResponseError(e);
			commError(e);
		}
	}

	private void refreshServerInfo() {
		if (!currentLogin.hasServer()) {
			throw new IllegalStateException("Need to be connected to a server");
		}

		try {
			exec.execute(new JSONObjectRequestExecutor<>(factory.serverInfoRequest(), new JsonConverters.ServerInfoConverter(), this, CallbackIds.SERVER_INFO));
		} catch (HttpRequestException e) {
			err.requestException(e);
			connectionFailure(e, ConnectionState.Invalid);
		}
	}

	private void serverInfoResult(RequestExecutor<ServerInfo, ?> r) {
		if (r.isSuccessful()) {
			serverInfo = r.getResult();

			if (pref != null)
				save().saveServerInfo().commit();

			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					for (IModelListener l: modelListeners) {
						l.onServerInfoChange(serverInfo);
					}
				}
			});
		} else {
			Exception e = r.getException();
			err.asyncTaskResponseError(e);
			commError(e);
		}
	}

	@Override
	public void onMissingGroupName(int groupID) {
		refreshAllUsers();
	}

	@Override
	public void onMissingUserName(int userID) {
		refreshAvailableGroups();
	}

	@Override
	public void saveModel() {
		if (pref == null)
			pref = getDefaultPreferences(context);

		save().saveLogin().saveServerConfig().saveChatrooms().saveConnectionStatus().saveGroups().saveServerInfo().commit();
		Log.i(THIS, "Saving Model");
	}

	@Override
	public URL getPictureUploadURL(String hash) {
		return factory.getPictureUploadURL(hash);
	}
	
//	@Override
//	public synchronized boolean isConnectionChanging() {
//		return state == ConnectionState.Connecting || state == ConnectionState.Disconnecting;
//	}
	
	@Override
	public synchronized boolean isConnected() {
		return state == ConnectionState.Connected;
	}

	/**
	 * Connected or InternalConnected
	 * InternalConnected will be set by Model after a login was successful, but not all information has been downloaded yet
	 * Some Modules would like to download independent data while / before other modules a downloading theirs
	 */
	synchronized boolean isConnectedInternal() {
		return state == ConnectionState.Connected || state == ConnectionState.InternalConnected;
	}
	
	@Override
	public ServerLogin getLogin() {
		return currentLogin;
	}

	@Override
	public GroupUser getUser() {
		return new GroupUser(currentLogin.getUserID(), currentLogin.getGroupID(), currentLogin.getName());
	}

	@Override
	public synchronized List<? extends IChatroom> getChatrooms() {
		if (state != ConnectionState.Connected || chatrooms == null) {
			throw new IllegalStateException("Chatrooms not available (State:"+ state +")");
		}

		return Collections.unmodifiableList(chatrooms);
	}


	@Override
	public synchronized IChatroom getChatroom(int id) {
		if (state != ConnectionState.Connected || chatrooms == null) {
			throw new IllegalStateException("Chatrooms not available (State:"+ state +")");
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
	public ITasks getTasks() {
		return tasks;
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
	public PictureIdResolver getPictureIdResolver() {
		return new PictureIdResolver(currentLogin.getServer().toString());
	}

	@Override
	public String getUrlFromImageId(int pictureID, PictureSize size) {
		return currentLogin.getServer().toString() + Paths.getPic(pictureID, size);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void executorResult(RequestExecutor<?, CallbackIds> r, CallbackIds callbackId) {
		switch (callbackId) {
		case CONFIG:
			serverConfigResult((RequestExecutor<ServerConfig, ?>) r);
			break;
		case LOGOUT:
			logoutResult((RequestExecutor<String, CallbackIds>) r);
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
		case USER_LIST:
			refreshAllUsersResult((RequestExecutor<List<GroupUser>, ?>) r);
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
			Log.v(THIS, "Saving Login:"+ currentLogin);

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
			Log.v(THIS, "Saving Chatrooms:"+ chatrooms);

			Chatroom.saveChatrooms(Model.this, chatrooms);
			
			return this;
		}
		
		public synchronized Saver saveConnectionStatus() {
			Log.v(THIS, "Saving ConnectionState:"+ state);

			edit.putBoolean(Std.CONNECTED, state == ConnectionState.Connected);
			
			return this;
		}
		
		public Saver saveServerConfig() {
			Log.v(THIS, "Saving ServerConfig:"+ serverConfig);

			edit.putString(Std.LATITUDE, String.valueOf(serverConfig.location.latitude));
			edit.putString(Std.LONGITUDE, String.valueOf(serverConfig.location.longitude));
			edit.putString(Std.SERVER+Std.NAME, serverConfig.name);
			edit.putInt(Std.ROUNDS, serverConfig.rounds);
			edit.putInt(Std.ROUND_TIME, serverConfig.roundTime);
			edit.putLong(Std.START_TIME, serverConfig.startTime);
			return this;
		}
		
		public void commit() {
			edit.commit();
		}

		public Saver saveGroups() {
			Log.v(THIS, "Saving Groups:"+ groups);

			db.delete(Groups.TABLE, null, null);

			for (Group g: groups) {
				ContentValues insert = new ContentValues();
				insert.put(Groups.KEY_ID, g.groupID);
				insert.put(Groups.KEY_NAME, g.name);
				insert.put(Groups.KEY_DESCRIPTION, g.description);
				db.insert(Groups.TABLE, null, insert);
			}

			groups = null;

			return this;
		}

		public Saver saveUsers(List<GroupUser> users) {
			Log.v(THIS, "Saving Users:"+ users);

			db.delete(Users.TABLE, null, null);

			for (GroupUser u: users) {
				ContentValues insert = new ContentValues();
				insert.put(Users.KEY_ID, u.userID);
				insert.put(Users.KEY_NAME, u.name);
				insert.put(Users.FOREIGN_GROUP, u.groupID);
				db.insert(Users.TABLE, null, insert);
			}

			return this;
		}

		public Saver saveServerInfo() {
			Log.v(THIS, "Saving ServerInfo:"+ serverInfo);

			edit.putString(Std.SERVER+Std.NAME, serverInfo.name);
			edit.putString(Std.SERVER+Std.DESCRIPTION, serverInfo.description);
			edit.putString(Std.SERVER + Std.API, serverInfo.getApiAsString());

			return this;
		}
	}

	private void restoreServerInfo() {
		ServerInfo s = ServerInfo.fromSet(
				pref.getString(Std.SERVER+Std.NAME, null),
				pref.getString(Std.SERVER+Std.DESCRIPTION, null),
				pref.getString(Std.SERVER+Std.API, ""));

		if (s.name != null && s.api.length > 0) {
			serverInfo = s;
			Log.i(THIS, "Server Info recovered");
		} else {
			serverInfo = null;
			Log.e(THIS, "Server Info corrupted");
			Log.v(THIS, pref.getString(Std.SERVER+Std.NAME, null) +"|"+
					pref.getString(Std.SERVER+Std.DESCRIPTION, null) +"|"+
					pref.getString(Std.SERVER+Std.API, ""));
		}
	}
	
	private void restoreServerConfig() {
		ServerConfig s = new ServerConfig(
				pref.getString(Std.SERVER+Std.NAME, null),
				Double.valueOf(pref.getString(Std.LATITUDE, "0")),
				Double.valueOf(pref.getString(Std.LONGITUDE, "0")),
				pref.getInt(Std.ROUNDS, 0),
				pref.getInt(Std.ROUND_TIME, 0),
				pref.getLong(Std.START_TIME, 0));
		
		if (s.isComplete()) {
			serverConfig = s;
			Log.i(THIS, "Server Config recovered");
		} else {
			serverConfig = null;
			Log.e(THIS, "ServerConfig corrupted");
			Log.v(THIS, pref.getString(Std.SERVER+Std.NAME, null) +"|"+
					Double.valueOf(pref.getString(Std.LATITUDE, "0")) +"|"+
					Double.valueOf(pref.getString(Std.LONGITUDE, "0")) +"|"+
					pref.getInt(Std.ROUNDS, 0) +"|"+
					pref.getInt(Std.ROUND_TIME, 0) +"|"+
					pref.getLong(Std.START_TIME, 0));
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
			Log.e(THIS, "Login corrupted", e);
			currentLogin = new ServerLogin();
			Log.v(THIS, pref.getString(Std.SERVER, null) +"|"+
					pref.getInt(Std.GROUP_ID, 0) +"|"+
					pref.getString(Std.USER+Std.NAME, null) +"|"+
					pref.getString(Std.GROUP+Std.PASSWORD, null) +"|"+
					pref.getLong(Std.LAST_LOGIN, 0) +"|"+
					auth);
		}
	}
	
	private synchronized void restoreConnectionStatus() {
		if (pref.getBoolean(Std.CONNECTED, false)) {
			state = ConnectionState.Connected;
			Log.i(THIS, "Status: Connected recovered");
		} else {
			state = ConnectionState.Disconnected;
			Log.i(THIS, "Status: Disconnected recovered");
		}
	}


	/**
	 * Called by parts of the Model, if their connection / communication fails, indicating a complete Network / Server failure
	 * Model needs to restart / reestablish connection / report to user
	 * @param exception the exception that caused the failure
	 */
	void commError(Exception exception) {
		if (exception instanceof ConnectException) {
			Log.e(THIS, "Failed to communicate with server! Going offline!");
		} else {
			Log.e(THIS, "Unknown Exception!");
		}
		if (currentLogin.isValid())
			connectionFailure(exception, ConnectionState.TemporaryNotAvailable);
		else
			connectionFailure(exception, ConnectionState.Invalid);
	}
	
	/**
	 * shutdown Executor (prevent it from accepting new CallbackIds but complete all previously accepted ones)
	 */
	@Override
	public void destroy() {
		Log.d(THIS, "Destroying Model: Closing DB, killing all tasks");

		save().saveChatrooms().commit();//TODO: maybe move someplace called more often
		
		db.close();
		db = null;

		exec.shutdown();
		exec = null;
	}
	
	@Override
	public void addListener(IModelListener l) {
		modelListeners.add(l);
	}
	
	@Override
	public void removeListener(IModelListener l) {
		modelListeners.remove(l);
	}

	/**
	 * Change the ConnectionState of the Model, broadcast the new state to all Listeners (except InternalConnected)
	 */
	private synchronized void setConnectionState(final ConnectionState newState) {
		if (newState == state) {
			Log.w(THIS, "Duplicate Status: "+ newState);
			return;
		}
		
		state = newState;
		
		Log.i(THIS, "Status: "+ newState);

		if (newState == ConnectionState.InternalConnected) // Only for Submodules of Model, that run between first login on server and "official" connected
			return;
		
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for(IModelListener l: modelListeners) {
					l.onConnectionStateChange(newState);
				}
			}
		});
	}

	/**
	 * Notify Listeners of a connection failure (e.g. HTTP Timeout)
	 * @param e Exception that caused the failure
	 * @param fallbackState a new ConnectionState (e.g. Disconnected / NoNetwork)
	 */
	private synchronized void connectionFailure(final Exception e, final ConnectionState fallbackState) {
		state = fallbackState;
		
		err.connectionFailure(e, fallbackState);
		
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for(IModelListener l: modelListeners) {
					l.onConnectionFailed(e, fallbackState);
				}
			}
		});
	}

	@Override
	public synchronized ConnectionState getConnectionState() {
		return state;
	}

	@Override
	public ServerInfo getServerInfo() {
		return serverInfo;
	}

	public ServerConfig getServerConfig() {
		return serverConfig;
	}

	@Override
	public List<Group> getAvailableGroups() {
		if (groups != null)
			return groups;
		else {
			Cursor c = db.query(Groups.TABLE, new String[]{Groups.KEY_ID, Groups.KEY_NAME, Groups.KEY_DESCRIPTION}, "", null, null, null, Groups.KEY_ID);
			List<Group> groups = new ArrayList<>();

			while (c.moveToNext()) {
				groups.add(new Group(c.getInt(0), c.getString(1), c.getString(2)));
			}
			c.close();

			return groups;
		}
	}
}