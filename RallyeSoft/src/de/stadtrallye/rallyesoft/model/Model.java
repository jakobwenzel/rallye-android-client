package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseArray;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.Std;
import de.stadtrallye.rallyesoft.communications.Pull.PendingRequest;
import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.communications.AsyncRequest;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.backend.DatabaseOpenHelper;
import de.stadtrallye.rallyesoft.util.JSONArray;
import de.stadtrallye.rallyesoft.util.JSONConverter;

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
	
	private static boolean DEBUG = false;
	
	public enum Tasks { LOGIN, CHAT_DOWNLOAD, CHECK_SERVER, MAP_NODES, CHAT_REFRESH };
	
	private int taskID = 0;

	private SharedPreferences pref;
	private RallyePull pull;
	private Context context;
	private SparseArray<Task<? extends Object>> callbacks;
	private String server;
	private int group;
	private String password;
	private String gcm;
	private boolean loggedIn;
	private ArrayList<IConnectionStatusListener> connectionListeners;
	private List<Chatroom> chatrooms;
	
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
		this.loggedIn = loggedIn;
		
		
		server = pref.getString(Std.SERVER, null);
		if (server == null) {
			this.loggedIn = false;
		} else {
			group = pref.getInt(Std.GROUP, 0);
			password = pref.getString(Std.PASSWORD, null);
			createChatrooms(extractChatRooms(pref.getString(Std.CHATROOMS, "")));
		}
		
		pull = new RallyePull(pref.getString(Std.SERVER, "FAIL"), gcm, context);
		
		callbacks = new SparseArray<Task<? extends Object>>();
		connectionListeners = new ArrayList<IConnectionStatusListener>();
	}
	
	
	@Override
	public void logout() {
		logout(null, 0);
	}

	public void logout(IModelResult<Boolean> ui, int tag) {
		try {
			new AsyncRequest(new Redirect<Boolean>(ui, true), tag).execute(pull.pendingLogout());
			loggedIn = false;
			connectionStatusChange();
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	@Override
	public void login(String server, String password, int group) {
		login(null, 0, server, group, password);
	}
	
	public void login(IModelResult<Boolean> ui, int tag, String server, int group, String password) {
		if (loggedIn) {
			err.notLoggedIn();
			return;
		}
		try {
			startAsyncTask(ui, tag, Tasks.LOGIN, RallyePull.pendingLogin(context, server, group, password, gcm));
			
			this.server = server;
			this.group = group;
			this.password = password;
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	public void retrieveCompleteChat(IModelResult<List<ChatEntry>> ui, int externalTag, int chatroom) {
		if (!loggedIn) {
			err.notLoggedIn();
			return;
		}
		try {
			startAsyncTask(ui, externalTag, Tasks.CHAT_DOWNLOAD, pull.pendingChatRefresh(chatroom, 0));
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	public void postChatMessage(IModelResult<List<ChatEntry>> ui, int externalTag, int chatroom, String msg) {
		if (!loggedIn) {
			err.notLoggedIn();
			return;
		}
		try {
			startAsyncTask(ui, externalTag, Tasks.CHAT_DOWNLOAD, pull.pendingChatPost(chatroom, msg, 0));
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	public void updateChat(IModelResult<List<ChatEntry>> ui, int externalTag, int chatroom, int beginningWith) {
		if (!loggedIn) {
			err.notLoggedIn();
			return;
		}
		
		try {
			startAsyncTask(ui, externalTag, Tasks.CHAT_REFRESH, pull.pendingChatRefresh(chatroom, beginningWith));
		} catch (RestException e) {
			err.restError(e);
		}
		
	}
	
	@Override
	public void checkConnectionStatus() {
		checkConnectionStatus(null, 0);	
	}
	
	public void checkConnectionStatus(IModelResult<Boolean> ui,	int externalTag) {
		try {
			startAsyncTask(ui, externalTag, Tasks.CHECK_SERVER, pull.pendingServerStatus(server));
		} catch (RestException e) {
			err.restError(e);
		}
		
	}
	
	public void getMapNodes(IModelResult<List<MapNode>> ui, int externalTag) {
		if (!loggedIn) {
			err.notLoggedIn();
			return;
		}
		try {
			startAsyncTask(ui, externalTag, Tasks.MAP_NODES, pull.pendingMapNodes());
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	private <T> void startAsyncTask(IModelResult<T> ui, int externalTag, Tasks internalTask, PendingRequest payload) {
		AsyncRequest p = new AsyncRequest(this, --taskID);
		callbacks.put(taskID, new Task<T>(ui, externalTag, internalTask, p));
		p.execute(payload);
	}
	
	public boolean isLoggedIn() {
		return loggedIn;
	}
	
	public String getServer() {
		return pref.getString(Std.SERVER, context.getString(R.string.default_server));
	}
	
	public int getGroup() {
		return pref.getInt(Std.GROUP, context.getResources().getInteger(R.integer.default_group));
	}
	
	public String getPassword() {
		return pref.getString(Std.PASSWORD, context.getString(R.string.default_password));
	}
	
	public List<Integer> getChatRooms() {
		List<Integer> l = new ArrayList<Integer>();
		for (Chatroom c: chatrooms) {
			l.add(c.getID());
		}
		return l;
	}
	
	/**
	 * 
	 * WARNING: do not change List
	 */
	@Override
	public List<Chatroom> getChatrooms() {
		return chatrooms;
	}
	
	private class Chatroom implements IChatroom {
		
		private int id;
		private String name;
		
		private List<IChatListener> listeners = new ArrayList<IChatListener>();
		
		@Deprecated
		public Chatroom(int id) {
			this(id, "Chatroom "+ id);
		}
		
		public Chatroom(int id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public int getID() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void adviseUse() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addListener(IChatListener l) {
			listeners.add(l);
		}

		@Override
		public void removeListener(IChatListener l) {
			listeners.remove(l);
		}

		@Override
		public List<ChatEntry> getChats() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addChat(String msg) {
			// TODO Auto-generated method stub
			
		}
	}
	
	@Override
	public String getUrlFromImageId(int pictureID, char size) {
		String res = getServer() +"/pic/get/"+ pictureID +"/"+ size;
//		Log.v(THIS, res);
		return res;
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public void onAsyncFinished(int internalTag, AsyncRequest task) {
		Tasks type = callbacks.get(internalTag).type;
		switch (type) {
		case LOGIN:
			boolean success = false;
			try {
				JSONConverter<Integer> conv = new JSONConverter<Integer>() {
					@Override
					public Integer doConvert(JSONObject o) throws JSONException {
						return o.getInt("chatroom");
					}
				};
				
				JSONArray<Integer> js = new JSONArray<Integer>(conv, task.get());
				
				ArrayList<Integer> res = js.toList();
				
				createChatrooms(res);
				
				success = true;
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
			
			if (success)
				logInSuccessfull();
			
			((Task<Boolean>) callbacks.get(internalTag)).callback(success);
			break;
		case CHAT_REFRESH:
		case CHAT_DOWNLOAD:
			try {
				((Task<List<ChatEntry>>) callbacks.get(internalTag)).callback(ChatEntry.translateJSON(task.get()));
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
			break;
		case CHECK_SERVER:
			try {
				String res = task.get();
				loggedIn = (task.getResponseCode() >= 200 && task.getResponseCode() < 300);
				if (loggedIn && callbacks.get(internalTag).externalTag != 0)
					((Task<Boolean>) callbacks.get(internalTag)).callback(loggedIn);
				
				connectionStatusChange();
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
			break;
		case MAP_NODES:
			try {
				((Task<List<MapNode>>) callbacks.get(internalTag)).callback(MapNode.translateJSON(task.get()));
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
		}
		callbacks.remove(internalTag);
	}
	
	private void logInSuccessfull() {
		loggedIn = true;
		saveLoginDetails(server, group, password, chatrooms);
		
		connectionStatusChange();
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
		for (int i = callbacks.size()-1; i>=0; --i) {
			callbacks.valueAt(i).task.cancel(true);
		}
	}
	
	/**
	 * Envelops one UniPush instance for callbacks
	 * @author Ramon
	 *
	 * @param <T>
	 */
	private class Task<T> {
		public IModelResult<T> ui;
		public int externalTag;
		public AsyncRequest task;
		public Tasks type;
		
		public Task(IModelResult<T> ui, int externalTag, Tasks type, AsyncRequest task) {
			this.ui = ui;
			this.externalTag = externalTag;
			this.task = task;
			this.type = type;
		}
		
		public void callback(T result) {
			ui.onModelFinished(externalTag, result);
		}
	}
	
	/**
	 * Redirects a finished Task to ui, with predetermined result
	 * Useful if I only want to signal, when the Task was completed
	 * @author Ramon
	 *
	 * @param <T>
	 */
	private class Redirect<T> implements IAsyncFinished {
		private IModelResult<T> ui;
		private T result;
		
		public Redirect(IModelResult<T> ui, T result) {
			this.ui = ui;
			this.result = result;
		}
		
		@Override
		public void onAsyncFinished(int tag, AsyncRequest task) {
			ui.onModelFinished(tag, result);
		}
	}
	
	private void createChatrooms(List<Integer> l) {
		chatrooms = new ArrayList<Chatroom>();
		for (Integer i: l) {
			chatrooms.add(new Chatroom(i));
		}
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
	
	private void connectionStatusChange() {
		for(IConnectionStatusListener l: connectionListeners) {
			l.onConnectionStatusChange(loggedIn);
		}
	}
	
	public static void enableDebugLogging() {
		DEBUG = true;
		AsyncRequest.enableDebugLogging();
	}

	public void testDB() {
		SQLiteOpenHelper helper = new DatabaseOpenHelper(context);
		SQLiteDatabase db = helper.getWritableDatabase();
		
	}
	
}
