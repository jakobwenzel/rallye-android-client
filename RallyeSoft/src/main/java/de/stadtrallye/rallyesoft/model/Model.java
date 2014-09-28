///*
// * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
// *
// * This file is part of RallyeSoft.
// *
// * RallyeSoft is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * Foobar is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
// */
//
//package de.stadtrallye.rallyesoft.model;
//
//import android.app.Activity;
//import android.content.ContentValues;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.content.SharedPreferences.Editor;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.os.Handler;
//import android.os.Looper;
//import android.provider.Settings;
//import android.util.Log;
//
//import java.net.ConnectException;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import de.rallye.model.structures.Group;
//import de.rallye.model.structures.GroupUser;
//import de.rallye.model.structures.LatLng;
//import de.rallye.model.structures.MapConfig;
//import de.rallye.model.structures.PictureSize;
//import de.rallye.model.structures.ServerInfo;
//import de.rallye.model.structures.UserAuth;
//import de.stadtrallye.rallyesoft.common.Std;
//import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
//import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
//import de.stadtrallye.rallyesoft.model.chat.IChatroom;
//import de.stadtrallye.rallyesoft.util.converters.JsonConverters;
//import de.stadtrallye.rallyesoft.util.converters.PreferenceConverters;
//import de.stadtrallye.rallyesoft.model.map.IMapManager;
//import de.stadtrallye.rallyesoft.model.map.MapManager;
//import de.stadtrallye.rallyesoft.model.tasks.ITaskManager;
//import de.stadtrallye.rallyesoft.model.tasks.TaskManager;
//import de.stadtrallye.rallyesoft.net.PictureIdResolver;
//import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper;
//import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Groups;
//import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Users;
//import de.stadtrallye.rallyesoft.util.executors.JSONArrayRequestExecutor;
//import de.stadtrallye.rallyesoft.util.executors.JSONObjectRequestExecutor;
//import de.stadtrallye.rallyesoft.util.executors.RequestExecutor;
//import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
//import de.stadtrallye.rallyesoft.net.Paths;
//import de.stadtrallye.rallyesoft.net.manual.RequestFactory;
//import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
//import de.stadtrallye.rallyesoft.util.PreferencesUtil;
//import de.wirsch.gcm.GcmHelper;
//
//import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_CHATROOMS;
//import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_GROUPS;
//import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_USERS;
//
///**
// * My Model
// * Should be the only Class to write to Preferences
// * All CallbackIds should start here
// * @author Ray
// *
// */
//public class Model implements IModel, RequestExecutor.Callback<Model.CallbackIds> {
//
//	// static
//	private static final String THIS = Model.class.getSimpleName();
//	private static final ErrorHandling err = new ErrorHandling(THIS);
//	@SuppressWarnings("unused")
//	final private static boolean DEBUG = false;
//
//	enum CallbackIds { LOGIN, LOGOUT, GROUP_LIST, SERVER_INFO, USER_LIST, AVAILABLE_CHATROOMS }
//
//	// Singleton Pattern
//	private static Model model;
//
//	// Android / UI Specific
//	private SharedPreferences pref;
//	final Context context;
//	final Handler uiHandler = new Handler(Looper.getMainLooper());
//
//	// State
//	private ConnectionState state;
//	private boolean refreshingGroups = false;
//	private boolean refreshingUsers = false;
//	private boolean refreshingChatrooms = false;
////	private int state;
//	private ServerLogin currentLogin;
//	private ServerInfo serverInfo;
//	private boolean hasGcm;
//
//	// Sub Modules
//	private List<Chatroom> chatrooms;
//	private final MapManager map;
//	private final TaskManager tasks;
//
//	// Listener
//	private final ArrayList<IModelListener> modelListeners = new ArrayList<IModelListener>();
//
//	// Temp
//	private List<Group> groups;// Needs to be stored here until we have preferences and can save it to Database
//
//	// Helper
//	final RequestFactory factory;
//	ExecutorService exec;
//	SQLiteDatabase db;
//	int deprecatedTables;
//
//	/**
//	 * If possible restore a Model from RAM or Settings
//	 * @param context needed for Database, Preferences, GCMid => give ApplicationContext to avoid leaking contexts during configuration changes
//	 * @return existing Model or one restored from settings
//	 */
//	public static IModel getInstance(Context context) {
//		if (model != null) {
//			return model.reuse();
//		} else {
//			return model = new Model(context, PreferencesUtil.getDefaultPreferences(context));
//		}
//	}
//
//	/**
//	 * Create a new Model, separate from the general Singleton Pattern of Model
//	 * @param context needed for Database, Preferences etc
//	 * @return a new completely empty Model, ready to connect to a new server
//	 */
//	public static IModel createTemporaryModel(Context context) {
//		return new Model(context, null);
//	}
//
//	/**
//	 * Helper Method to get the Model from a parent Activity
//	 * @param modelActivity the parent Activity who must initialize the Model
//	 * @return the current Model
//	 */
//	public static IModel getModel(Activity modelActivity) {
//		try {
//			return ((IModelActivity) modelActivity).getModel();
//		} catch (ClassCastException e) {
//			Log.e(THIS, "The Activity "+ modelActivity +" must implement IModelActivity", e);
//			throw new IllegalArgumentException("The Activity must implement IModelActivity");
//		}
//	}
//
//	/**
//	 * Switch the new Model from {@link #createTemporaryModel(android.content.Context)} in to the Singleton Pattern of Model after it has been tested
//	 * Will kill and terminate the old Model immediately
//	 * @param newModel a Model whose connection to a new server was successful
//	 */
//	private static void switchToNew(Model newModel) {
//		if (model != null) {
//			if (model.currentLogin != null && model.currentLogin.getServer() != null && !model.currentLogin.getServer().equals(newModel.currentLogin.getServer()))
//				model.logout();
//
//			model.destroy();
//		}
//
//		Model.model = newModel;
//
//		model.saveModel();
//	}
//
//	@Override
//	public void acceptTemporaryModel() {
//		Model.switchToNew(this);
//		Log.i(THIS, "Switched to new Model");
//	}
//
//	@Override
//	public boolean isTemporary() {
//		return pref == null;
//	}
//
//	@Override
//	public boolean isEmpty() {
//		return !currentLogin.hasServer();
//	}
//
//	/**
//	 * Ensure that this Model has a working DB Connection and Executor (if for some reason the server was destroyed, but the context (and static server with it) survived)
//	 * @return for convenience
//	 */
//	private Model reuse() {
//		if (db == null) {
//			Log.e(THIS, "Reusing old Model: reopening DB");
//			initDatabase();
//		}
//		if (exec == null) {
//			Log.e(THIS, "Reusing old Model: restarting Executor");
//			initExecutor();
//		}
//
//		return this;
//	}
//
//	private void initExecutor() {
//		exec = Executors.newCachedThreadPool();
//	}
//
//	private void initDatabase() {
//		DatabaseHelper helper = new DatabaseHelper(context);
//		db = helper.getWritableDatabase();
//
//		deprecatedTables = helper.getEditedTables();
//	}
//
//	private Model(Context context, SharedPreferences pref) {
//		this.pref = pref;
//		this.context = context;
//
//
//		initDatabase();
//
//		initExecutor();
//
//		if (pref != null) {
//			try {
//				restoreLogin();
//				restoreServerInfo();
//				restoreConnectionStatus();
//			} catch (Exception e) {
//				Log.e(THIS, "Corrupted / Incomplete Login-Data: staying offline", e);
//				if (currentLogin == null)
//					currentLogin = new ServerLogin();
//				setConnectionState(ConnectionState.None);
//			}
//		} else {
//			currentLogin = new ServerLogin();
//			setConnectionState(ConnectionState.None);
//		}
//
//		String uniqueID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
//		factory = new RequestFactory(currentLogin, uniqueID);
//		String gcm = GcmHelper.getGcmId();
//
//		hasGcm = gcm != null;
//		if (hasGcm) {
//			factory.setPushID(gcm);
//		}
//
//		map = new MapManager(this);
//		tasks = new TaskManager(this);
//
//		if (state == ConnectionState.Connected) {
//			if ((deprecatedTables & EDIT_CHATROOMS) > 0) {
//				refreshAvailableChatrooms();
//				deprecatedTables &= ~EDIT_CHATROOMS;
//			} else {
//				try {
//					chatrooms = Chatroom.getChatrooms(this);
//				} catch (Exception e) {
//					Log.e(THIS, "Chatrooms could not be restored");
//					refreshAvailableChatrooms();
//				}
//			}
//			if ((deprecatedTables & EDIT_USERS) > 0) {
//				refreshAllUsers();
//				deprecatedTables &= ~EDIT_USERS;
//			}
//			if ((deprecatedTables & EDIT_GROUPS) > 0) {
//				refreshAvailableGroups();
//				deprecatedTables &= ~EDIT_GROUPS;
//			}
//		}
//	}
//
//	@Override
//	public synchronized void logout() {
//		if (currentLogin == null || state != ConnectionState.Connected) {
//			Log.w(THIS, "Cannot logout: Not logged in");
//			return;
//		}
//
//		setConnectionState(ConnectionState.Disconnecting);
//		if (pref != null)
//			save().saveConnectionStatus().commit();
//
//		try {
//			exec.execute(new RequestExecutor<String, CallbackIds>(factory.logoutRequest(), null, this, CallbackIds.LOGOUT));
//		} catch (Exception e) {
//			err.requestException(e);
//		}
//	}
//
//	@SuppressWarnings("UnusedParameters")
//	private void logoutResult(RequestExecutor<String, ?> r) {
//		currentLogin.setUserAuth(null);
//		setConnectionState(ConnectionState.Disconnected);
//	}
//
//	@Override
//	public synchronized String setServer(String server) throws MalformedURLException {
//		if (!server.endsWith("/"))
//			server = server +"/";
//
//		if (!(state == ConnectionState.None || state == ConnectionState.Invalid))
//			throw new IllegalStateException("This Model already has a Server");
//
//		this.currentLogin.setServer(new URL(server));
//
////		setConnectionState(ConnectionState.Disconnected);
//
//		Log.i(THIS, "Model has now a Server, getting information...");
//
//		refreshServerInfo();
//		refreshAvailableGroups();
//		return server;
//	}
//
//	private synchronized void login() {
//		setConnectionState(ConnectionState.Connecting);
//
//		Log.i(THIS, "Reconnecting");
//
//		try {
////			factory.setPushID(GCMRegistrar.getRegistrationId(context));
//			exec.execute(new JSONObjectRequestExecutor<UserAuth, CallbackIds>(factory.loginRequest(), new ServerLogin.AuthConverter(), this, CallbackIds.LOGIN));
//		} catch (Exception e) {
//			err.requestException(e);
//			connectionFailure(e, ConnectionState.Invalid);
//		}
//	}
//
//	@Override
//	public synchronized void login(final String username, final int groupID, final String groupPassword) {
//		if (state != ConnectionState.None) {
//			throw new IllegalStateException("Already connected");
//		}
//
//		Log.i(THIS, "Logging in");
//
//		if (!hasGcm) {
//			Log.w(THIS, "GCM ID still empty... delaying until ready...");
//			GcmHelper.setGcmListener(new GcmHelper.IGcmListener() {
//				@Override
//				public void onDelayedGcmId(String gcmId) {
//					Log.i(THIS, "GCM ID ready!");
//					login(username, groupID, groupPassword);
//				}
//			});
//		}
//
//		setConnectionState(ConnectionState.Connecting);
//		currentLogin.setName(username);
//		currentLogin.setGroupID(groupID);
//		currentLogin.setGroupPassword(groupPassword);
//
//		try {
////			factory.setPushID(GCMRegistrar.getRegistrationId(context));// newly installed GCM_ID has on occasion not been available at first start
//			exec.execute(new JSONObjectRequestExecutor<UserAuth, CallbackIds>(factory.loginRequest(), new ServerLogin.AuthConverter(), this, CallbackIds.LOGIN));
//		} catch (Exception e) {
//			err.requestException(e);
//			connectionFailure(e, ConnectionState.Invalid);
//		}
//
//	}
//
//	void loginResult(RequestExecutor<UserAuth, ?> r) {
//		if (r.isSuccessful()) {
//
//			currentLogin.setUserAuth(r.getResult());
//			currentLogin.validated();
//			setConnectionState(ConnectionState.InternalConnected);
//
//			refreshConfiguration();
//			map.updateMapConfig();
//			map.updateMap();
//			tasks.update();
//		} else {
//			connectionFailure(r.getException(), ConnectionState.Invalid);
//		}
//	}
//
//	@Override
//	public synchronized void reconnect() {
//		if (!canReconnect()) {
//			throw new IllegalStateException("Cannot reconnect, not enough information");
//		}
//
//		if (currentLogin.hasUserAuth()) {
//			setConnectionState(ConnectionState.InternalConnected);
//			refreshConfiguration();
//		} else {
//			login();
//		}
//	}
//
//	@Override
//	public synchronized boolean canReconnect() {
//		return (state == ConnectionState.Disconnected || state == ConnectionState.TemporaryNotAvailable) && currentLogin.isValid();
//	}
//
//	@Override
//	public synchronized void refreshConfiguration() {
//		if (!isConnectedInternal()) {
//			throw new IllegalStateException("refreshConfiguration needs at least State=InternalConnected");
//		}
//
//        refreshAvailableChatrooms();
////        map.updateMapConfig();
//	}
//
//	private void refreshAvailableChatrooms() {
//		synchronized(this) {
//			if (refreshingChatrooms) {
//				Log.w(THIS, "Preventing concurrent Chatroom refreshes");
//				return;
//			}
//			refreshingChatrooms = true;
//		}
//
//		try {
//			Log.d(THIS, "getting available chatrooms");
//			exec.execute(new JSONArrayRequestExecutor<Chatroom, CallbackIds>(factory.availableChatroomsRequest(), new JsonConverters.ChatroomConverter(this), this, CallbackIds.AVAILABLE_CHATROOMS));
//		} catch (HttpRequestException e) {
//			err.requestException(e);
//			connectionFailure(e, ConnectionState.Invalid);
//		}
//	}
//
//	private void availableChatroomsResult(RequestExecutor<List<Chatroom>, ?> r) {
//		if (r.isSuccessful()) {
//				List<Chatroom> chatrooms = r.getResult();
//
//                if (!chatrooms.equals(this.chatrooms)) {
//                    this.chatrooms = chatrooms;
//					if (pref != null)
//						save().saveChatrooms().commit();
//					Log.d(THIS, "Chatroom Config has changed, replacing");
//                }
//
//				checkConnectionComplete();
//		} else {
//			Exception e = r.getException();
//			err.asyncTaskResponseError(e);
//			commError(e);
//		}
//
//		synchronized (this) {
//			refreshingChatrooms = false;
//		}
//	}
//
//	private synchronized void checkConnectionComplete() {
//		if ((state == ConnectionState.Connecting || state == ConnectionState.InternalConnected) && chatrooms != null) {
//			setConnectionState(ConnectionState.Connected);
//
//			if (pref != null)
//				save().saveConnectionStatus().commit();
//		}
//	}
//
//	@Override
//	public void refreshAvailableGroups() {
//		if (!currentLogin.hasServer()) {
//			throw new IllegalStateException("Cannot request available groups without setServer() first");
//		}
//
//		synchronized(this) {
//			if (refreshingGroups) {
//				Log.w(THIS, "Preventing concurrent Group refreshes");
//				return;
//			}
//			refreshingGroups = true;
//		}
//
//		try {
//			exec.execute(new JSONArrayRequestExecutor<Group, CallbackIds>(factory.availableGroupsRequest(), new JsonConverters.GroupConverter(), this, CallbackIds.GROUP_LIST));
//		} catch (HttpRequestException e) {
//			err.requestException(e);
//			connectionFailure(e, ConnectionState.Invalid);
//		}
//	}
//
//	private void availableGroupsResult(RequestExecutor<List<Group>, ?> r) {
//		if (r.isSuccessful()) {
//			groups = r.getResult();
//			Log.d(THIS, "Received available groups: "+ groups);
//			Collections.sort(groups, new Comparator<Group>() {
//				@Override
//				public int compare(Group lhs, Group rhs) {
//					return (lhs.groupID > rhs.groupID)? 1 : (lhs.groupID < rhs.groupID)? -1 : 0;
//				}
//			});
//
//
//			if (pref != null)
//				save().saveGroups().commit();
//
//			uiHandler.post(new Runnable() {
//				@Override
//				public void run() {
//					for (IModelListener l: modelListeners) {
//						l.onAvailableGroupsChange(groups);
//					}
//				}
//			});
//		}
//
//		synchronized (this) {
//			refreshingGroups = false;
//		}
//	}
//
//	private synchronized void refreshAllUsers() {
//		if (state != ConnectionState.Connected) {
//			throw new IllegalStateException("Need to be connected to a server");
//		}
//
//		synchronized(this) {
//			if (refreshingUsers) {
//				Log.w(THIS, "Preventing concurrent User refreshes");
//				return;
//			}
//			refreshingUsers = true;
//		}
//
//		try {
//			exec.execute(new JSONArrayRequestExecutor<GroupUser, CallbackIds>(factory.allUsersRequest(), new JsonConverters.GroupUserConverter(), this, CallbackIds.USER_LIST));
//		} catch (HttpRequestException e) {
//			err.requestException(e);
//			connectionFailure(e, ConnectionState.Invalid);
//		}
//	}
//
//	private void refreshAllUsersResult(RequestExecutor<List<GroupUser>, ?> r) {
//		if (r.isSuccessful()) {
//			final List<GroupUser> users = r.getResult();
//
//			save().saveUsers(users).commit();
//
//			for (Chatroom c: chatrooms) {
//				c.onDbChange();
//			}
//		} else {
//			Exception e = r.getException();
//			err.asyncTaskResponseError(e);
//			commError(e);
//		}
//		synchronized (this) {
//			refreshingUsers = false;
//		}
//	}
//
//	private void refreshServerInfo() {
//		if (!currentLogin.hasServer()) {
//			throw new IllegalStateException("Need to be connected to a server");
//		}
//
//		try {
//			exec.execute(new JSONObjectRequestExecutor<ServerInfo, CallbackIds>(factory.serverInfoRequest(), new JsonConverters.ServerInfoConverter(), this, CallbackIds.SERVER_INFO));
//		} catch (HttpRequestException e) {
//			err.requestException(e);
//			connectionFailure(e, ConnectionState.Invalid);
//		}
//	}
//
//	private void serverInfoResult(RequestExecutor<ServerInfo, ?> r) {
//		if (r.isSuccessful()) {
//			Log.i(THIS, "Successfully received ServerInfo");
//			serverInfo = r.getResult();
//
//			if (pref != null)
//				save().saveServerInfo().commit();
//
//			uiHandler.post(new Runnable() {
//				@Override
//				public void run() {
//					for (IModelListener l: modelListeners) {
//						l.onServerInfoChange(serverInfo);
//					}
//				}
//			});
//		} else {
//			Exception e = r.getException();
//			err.asyncTaskResponseError(e);
//			commError(e);
//		}
//		synchronized (this) {
//			refreshingUsers = false;
//		}
//	}
//
//	@Override
//	public void onMissingGroupName(int groupID) {
//		refreshAllUsers();
//	}
//
//	@Override
//	public void onMissingUserName(int userID) {
//		Log.i(THIS,"Missing User name for "+userID);
//		refreshAllUsers();
//	}
//
//	@Override
//	public void saveModel() {
//		if (pref == null) {
//			//throw new RuntimeException("No Preferences to save to...");
//			pref = PreferencesUtil.getDefaultPreferences(context);
//		}
//
//		save().saveLogin().saveMapConfig().saveChatrooms().saveConnectionStatus().saveGroups().saveServerInfo().commit();
//		Log.i(THIS, "Saving Model");
//	}
//
//	@Override
//	public URL getPictureUploadURL(String hash) {
//		return factory.getPictureUploadURL(hash);
//	}
//
////	@Override
////	public synchronized boolean isConnectionChanging() {
////		return state == ConnectionState.Connecting || state == ConnectionState.Disconnecting;
////	}
//
//	@Override
//	public synchronized boolean isConnected() {
//		return state == ConnectionState.Connected;
//	}
//
//	/**
//	 * Connected or InternalConnected
//	 * InternalConnected will be set by Model after a login was successful, but not all information has been downloaded yet
//	 * Some Modules would like to download independent data while / before other modules a downloading theirs
//	 */
//	synchronized boolean isConnectedInternal() {
//		return state == ConnectionState.Connected || state == ConnectionState.InternalConnected;
//	}
//
//	@Override
//	public ServerLogin getLogin() {
//		return currentLogin;
//	}
//
//	@Override
//	public GroupUser getUser() {
//		return new GroupUser(currentLogin.getUserID(), currentLogin.getGroupID(), currentLogin.getName());
//	}
//
//	@Override
//	public synchronized List<? extends IChatroom> getChatrooms() {
//		if (state != ConnectionState.Connected || chatrooms == null) {
//			throw new IllegalStateException("Chatrooms not available (State:"+ state +")");
//		}
//
//		return Collections.unmodifiableList(chatrooms);
//	}
//
//
//	@Override
//	public synchronized IChatroom getChatroom(int id) {
//		if (state != ConnectionState.Connected || chatrooms == null) {
//			throw new IllegalStateException("Chatrooms not available (State:"+ state +")");
//		}
//
//		for (IChatroom r: chatrooms) {
//			if (r.getID() == id)
//			{
//				return r;
//			}
//		}
//
//		return null;
//	}
//
//	@Override
//	public IMapManager getMap() {
//		return map;
//	}
//
//	@Override
//	public ITaskManager getTasks() {
//		return tasks;
//	}
//
//	@Override
//	public String getAvatarURL(int groupID) {
//		return currentLogin.getServer().toString() + Paths.getAvatar(groupID);
//	}
//
//	@Override
//	public String getServerPictureURL() {
//		return currentLogin.getServer().toString() + Paths.SERVER_PICTURE;
//	}
//
//	@Override
//	public PictureIdResolver getPictureIdResolver() {
//		return new PictureIdResolver(currentLogin.getServer().toString());
//	}
//
//	@Override
//	public String getUrlFromImageId(int pictureID, PictureSize size) {
//		return currentLogin.getServer().toString() + Paths.getPic(pictureID, size);
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public void executorResult(RequestExecutor<?, CallbackIds> r, CallbackIds callbackId) {
//		switch (callbackId) {
//		case LOGOUT:
//			logoutResult((RequestExecutor<String, CallbackIds>) r);
//			break;
//		case GROUP_LIST:
//			availableGroupsResult((RequestExecutor<List<Group>, ?>) r);
//			break;
//		case AVAILABLE_CHATROOMS:
//			availableChatroomsResult((RequestExecutor<List<Chatroom>, ?>) r);
//			break;
//		case LOGIN:
//			loginResult((RequestExecutor<UserAuth, ?>) r);
//			break;
//		case SERVER_INFO:
//			serverInfoResult((RequestExecutor<ServerInfo, ?>) r);
//			break;
//		case USER_LIST:
//			refreshAllUsersResult((RequestExecutor<List<GroupUser>, ?>) r);
//			break;
//		default:
//			Log.e(THIS, "Unknown Executor Callback");
//			break;
//
//		}
//	}
//
//	Saver save() {
//		return new Saver();
//	}
//
//	class Saver {
//		private final Editor edit;
//
//		public Saver() {
//			this.edit = pref.edit();
//		}
//
//		public Saver saveLogin() {
//			Log.v(THIS, "Saving Login:"+ currentLogin);
//
//			edit.putString(Std.SERVER, currentLogin.getServer().toString());
//			edit.putInt(Std.GROUP_ID, currentLogin.getGroupID());
//			edit.putString(Std.GROUP+Std.PASSWORD, currentLogin.getGroupPassword());
//			edit.putLong(Std.LAST_LOGIN, currentLogin.getLastValidated());
//			edit.putString(Std.USER+Std.NAME, currentLogin.getName());
//			edit.putInt(Std.USER_ID, currentLogin.getUserID());
//			edit.putString(Std.USER+Std.PASSWORD, currentLogin.getUserPassword());
//			return this;
//		}
//
//		public Saver saveChatrooms() {
//			Log.v(THIS, "Saving Chatrooms:"+ chatrooms);
//
//			Chatroom.saveChatrooms(Model.this, chatrooms);
//
//			return this;
//		}
//
//		public synchronized Saver saveConnectionStatus() {
//			Log.v(THIS, "Saving ConnectionState:"+ state);
//
//			edit.putBoolean(Std.CONNECTED, state == ConnectionState.Connected);
//
//			return this;
//		}
//
//		public Saver saveMapConfig() {
//			MapConfig mapConfig = map.getMapConfig();
//			Log.v(THIS, "Saving MapConfig:"+ mapConfig);
//
//			if (mapConfig == null)
//				Log.e(THIS, "MapConfig still null during Save!");
//			else {
//
//				edit.putString(Std.MAP_BOUNDS+Std.LATITUDE, String.valueOf(mapConfig.location.latitude));
//				edit.putString(Std.MAP_BOUNDS+Std.LONGITUDE, String.valueOf(mapConfig.location.longitude));
//				edit.putString(Std.MAP_BOUNDS+Std.NAME, mapConfig.name);
//				edit.putFloat(Std.MAP_BOUNDS+ Std.ZOOM, mapConfig.zoomLevel);
//				edit.putString(Std.MAP_BOUNDS + Std.MAP_BOUNDS, PreferenceConverters.toSingleString(mapConfig.getBoundsAsSet()));
//			}
//
//			return this;
//		}
//
//		public void commit() {
//			edit.apply();
//		}
//
//		public Saver saveGroups() {
//			Log.v(THIS, "Saving Groups:"+ groups);
//
//			db.delete(Groups.TABLE, null, null);
//
//			for (Group g: groups) {
//				ContentValues insert = new ContentValues();
//				insert.put(Groups.KEY_ID, g.groupID);
//				insert.put(Groups.KEY_NAME, g.name);
//				insert.put(Groups.KEY_DESCRIPTION, g.description);
//				db.insert(Groups.TABLE, null, insert);
//			}
//
//			groups = null;
//
//			return this;
//		}
//
//		public Saver saveUsers(List<GroupUser> users) {
//			Log.v(THIS, "Saving Users:"+ users);
//
//			db.delete(Users.TABLE, null, null);
//
//			for (GroupUser u: users) {
//				ContentValues insert = new ContentValues();
//				insert.put(Users.KEY_ID, u.userID);
//				insert.put(Users.KEY_NAME, u.name);
//				insert.put(Users.FOREIGN_GROUP, u.groupID);
//				db.insert(Users.TABLE, null, insert);
//			}
//
//			return this;
//		}
//
//		public Saver saveServerInfo() {
//			Log.v(THIS, "Saving ServerInfo:"+ serverInfo);
//
//			edit.putString(Std.SERVER+Std.NAME, serverInfo.name);
//			edit.putString(Std.SERVER+Std.DESCRIPTION, serverInfo.description);
//			edit.putString(Std.SERVER + Std.API, serverInfo.getApiAsString());
//            edit.putString(Std.SERVER+Std.BUILD, serverInfo.build.toString());
//
//			return this;
//		}
//	}
//
//	private void restoreServerInfo() {
//		if (!pref.contains(Std.SERVER+Std.NAME)) {
//			serverInfo = null;
//			Log.w(THIS, "No Server Info found");
//			return;
//		}
//
//		ServerInfo s = ServerInfo.fromSet(
//				pref.getString(Std.SERVER+Std.NAME, null),
//				pref.getString(Std.SERVER+Std.DESCRIPTION, null),
//				pref.getString(Std.SERVER+Std.API, ""),
//                pref.getString(Std.SERVER+Std.BUILD, "NONE"));
//
//		if (s.name != null && s.api.length > 0) {
//			serverInfo = s;
//			Log.i(THIS, "Server Info recovered");
//		} else {
//			serverInfo = null;
//			Log.e(THIS, "Server Info corrupted");
//			Log.v(THIS, pref.getString(Std.SERVER+Std.NAME, null) +"|"+
//					pref.getString(Std.SERVER+Std.DESCRIPTION, null) +"|"+
//					pref.getString(Std.SERVER+Std.API, ""));
//		}
//	}
//
//	MapConfig restoreMapConfig() {
//		if (!pref.contains(Std.MAP_BOUNDS+Std.NAME)) {
//			Log.w(THIS, "No Map Config found");
//			return null;
//		}
//
//		MapConfig s = new MapConfig(
//				pref.getString(Std.MAP_BOUNDS+Std.NAME, ""),
//                new LatLng(Double.valueOf(pref.getString(Std.MAP_BOUNDS+Std.LATITUDE, "0")), Double.valueOf(pref.getString(Std.MAP_BOUNDS+Std.LONGITUDE, "0"))),
//				pref.getFloat(Std.MAP_BOUNDS+Std.ZOOM, 0),
//                MapConfig.getBounds(PreferenceConverters.fromSingleString(pref.getString(Std.MAP_BOUNDS+Std.MAP_BOUNDS, ""))));
//
//        if (s.zoomLevel != 0) {
//			Log.i(THIS, "Server Config recovered");
//            return s;
//		} else {
//			Log.e(THIS, "ServerConfig corrupted");
//			Log.v(THIS, pref.getString(Std.MAP_BOUNDS+Std.NAME, null) +"|"+
//					pref.getString(Std.MAP_BOUNDS+Std.LATITUDE, null) +"|"+
//					pref.getString(Std.MAP_BOUNDS+Std.LONGITUDE, null) +"|"+
//					pref.getFloat(Std.MAP_BOUNDS+Std.ZOOM, 0) +"|"+
//					pref.getString(Std.MAP_BOUNDS+Std.MAP_BOUNDS, null));
//            return null;
//		}
//	}
//
//	private void restoreLogin() {
//		if (!pref.contains(Std.SERVER)) {
//			Log.w(THIS, "No Login found");
//			currentLogin = new ServerLogin();
//			return;
//		}
//
//		UserAuth auth = new UserAuth(pref.getInt(Std.USER_ID, 0), pref.getString(Std.USER+Std.PASSWORD, null));
//		if (auth.password == null)
//			auth = null;
//
//		try {
//			currentLogin = new ServerLogin(pref.getString(Std.SERVER, null),
//					pref.getInt(Std.GROUP_ID, 0),
//					pref.getString(Std.USER+Std.NAME, null),
//					pref.getString(Std.GROUP+Std.PASSWORD, null),
//					pref.getLong(Std.LAST_LOGIN, 0),
//					auth);
//			Log.i(THIS, "Login recovered");
//		} catch (MalformedURLException e) {
//			Log.e(THIS, "Login corrupted", e);
//			currentLogin = new ServerLogin();
//			Log.v(THIS, pref.getString(Std.SERVER, null) +"|"+
//					pref.getInt(Std.GROUP_ID, 0) +"|"+
//					pref.getString(Std.USER+Std.NAME, null) +"|"+
//					pref.getString(Std.GROUP+Std.PASSWORD, null) +"|"+
//					pref.getLong(Std.LAST_LOGIN, 0) +"|"+
//					auth);
//		}
//	}
//
//	private synchronized void restoreConnectionStatus() {
//		if (pref.getBoolean(Std.CONNECTED, false)) {
//			state = ConnectionState.Connected;
//			Log.i(THIS, "Status: Connected recovered");
//		} else {
//			state = ConnectionState.Disconnected;
//			Log.i(THIS, "Status: Disconnected recovered");
//		}
//	}
//
//
//	/**
//	 * Called by parts of the Model, if their connection / communication fails, indicating a complete Network / Server failure
//	 * Model needs to restart / reestablish connection / report to user
//	 * @param exception the exception that caused the failure
//	 */
//	void commError(Exception exception) {
//		if (exception instanceof ConnectException) {
//			Log.e(THIS, "Failed to communicate with server! Going offline!");
//		} else {
//			Log.e(THIS, "Unknown Exception!");
//		}
//		if (currentLogin.isValid())
//			connectionFailure(exception, ConnectionState.TemporaryNotAvailable);
//		else
//			connectionFailure(exception, ConnectionState.Invalid);
//	}
//
//	@Override
//	public void saveState() {
//		if (!isTemporary())// save everything that changes without explicit refreshing originiating here
//			save().saveChatrooms().commit();
//	}
//
//	/**
//	 * shutdown Executor (prevent it from accepting new CallbackIds but complete all previously accepted ones)
//	 */
//	@Override
//	public void destroy() {
//		Log.d(THIS, "Destroying Model: Closing DB, killing all tasks");
//
//		db.close();
//		db = null;
//
//		exec.shutdown();
//		exec = null;
//	}
//
//	@Override
//	public void addListener(IModelListener l) {
//		modelListeners.add(l);
//	}
//
//	@Override
//	public void removeListener(IModelListener l) {
//		modelListeners.remove(l);
//	}
//
//	/**
//	 * Change the ConnectionState of the Model, broadcast the new state to all Listeners (except InternalConnected)
//	 */
//	private synchronized void setConnectionState(final ConnectionState newState) {
//		if (newState == state) {
//			Log.w(THIS, "Duplicate Status: "+ newState);
//			return;
//		}
//
//		state = newState;
//
//		Log.i(THIS, "Status: "+ newState);
//
//		if (newState == ConnectionState.InternalConnected) // Only for Submodules of Model, that run between first login on server and "official" connected
//			return;
//
//		uiHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				for(IModelListener l: modelListeners) {
//					l.onConnectionStateChange(newState);
//				}
//			}
//		});
//	}
//
//	/**
//	 * Notify Listeners of a connection failure (e.g. HTTP Timeout)
//	 * @param e Exception that caused the failure
//	 * @param fallbackState a new ConnectionState (e.g. Disconnected / NoNetwork)
//	 */
//	private synchronized void connectionFailure(final Exception e, final ConnectionState fallbackState) {
//		state = fallbackState;
//
//		err.connectionFailure(e, fallbackState);
//
//		uiHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				for(IModelListener l: modelListeners) {
//					l.onConnectionFailed(e, fallbackState);
//				}
//			}
//		});
//	}
//
//	@Override
//	public synchronized ConnectionState getConnectionState() {
//		return state;
//	}
//
//	@Override
//	public ServerInfo getServerInfo() {
//		return serverInfo;
//	}
//
//	@Override
//	public List<Group> getAvailableGroups() {
//		if (groups != null)
//			return groups;
//		else {
//			Cursor c = db.query(Groups.TABLE, new String[]{Groups.KEY_ID, Groups.KEY_NAME, Groups.KEY_DESCRIPTION}, "", null, null, null, Groups.KEY_ID);
//			List<Group> groups = new ArrayList<Group>();
//
//			while (c.moveToNext()) {
//				groups.add(new Group(c.getInt(0), c.getString(1), c.getString(2)));
//			}
//			c.close();
//
//			return groups;
//		}
//	}
//}
