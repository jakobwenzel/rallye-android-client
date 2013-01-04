package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
	
	private static boolean DEBUG = false;
	
	public enum Tasks { LOGIN, CHAT_DOWNLOAD, CHECK_SERVER, MAP_NODES, CHAT_REFRESH };
	
	private int taskID = 0;

	private SharedPreferences pref;
	RallyePull pull;
	private Context context;
	SparseArray<Task> callbacks;
	private String server;
	private int group;
	private String password;
	private String gcm;
	boolean loggedIn;
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
			chatrooms = Chatroom.getChatrooms((extractChatRooms(pref.getString(Std.CHATROOMS, ""))), this);
		}
		
		pull = new RallyePull(pref.getString(Std.SERVER, "FAIL"), gcm, context);
		
		callbacks = new SparseArray<Task>();
		connectionListeners = new ArrayList<IConnectionStatusListener>();
	}
	
	
	@Override
	public void logout() {
		logout(null, 0); //TODO implement directly
	}

	public void logout(IModelResult<Boolean> ui, int tag) {
		try {
			new AsyncRequest<String>(new Redirect<Boolean>(ui, true), tag, null).execute(pull.pendingLogout());
			loggedIn = false;
			connectionStatusChange();
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	private static class LoginConverter extends JSONConverter<Integer> {
		@Override
		public Integer doConvert(JSONObject o) throws JSONException {
			return o.getInt("chatroom");
		}
	}
	
	@Override
	public void login(String server, String password, int group) {
		login(null, 0, server, group, password); //TODO implement directly
	}
	
	public void login(IModelResult<Boolean> ui, int tag, String server, int group, String password) {
		if (loggedIn) {
			err.loggedIn();
			return;
		}
		try {
			startAsyncTask(Tasks.LOGIN, RallyePull.pendingLogin(context, server, group, password, gcm),
					new StringedJSONArrayConverter<Integer>(new LoginConverter()),
					ui);
			
			this.server = server;
			this.group = group;
			this.password = password;
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	@Deprecated
	public void retrieveCompleteChat(IModelResult<List<ChatEntry>> ui, int externalTag, int chatroom) {
		if (!loggedIn) {
			err.notLoggedIn();
			return;
		}
		try {
			startAsyncTask(Tasks.CHAT_DOWNLOAD, pull.pendingChatRefresh(chatroom, 0), new StringedJSONArrayConverter<ChatEntry>(new ChatEntry.ChatConverter()), ui);
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	@Deprecated
	public void postChatMessage(IModelResult<List<ChatEntry>> ui, int externalTag, int chatroom, String msg) {
        if (!loggedIn) {
                err.notLoggedIn();
                return;
        }
        try {
                startAsyncTask(Tasks.CHAT_DOWNLOAD, pull.pendingChatPost(chatroom, msg, 0), null, ui);
        } catch (RestException e) {
                err.restError(e);
        }
	}
	
	@Deprecated
	public void updateChat(IModelResult<List<ChatEntry>> ui, int externalTag, int chatroom, int beginningWith) {
		if (!loggedIn) {
			err.notLoggedIn();
			return;
		}
		
		try {
			startAsyncTask(Tasks.CHAT_REFRESH, pull.pendingChatRefresh(chatroom, beginningWith), new StringedJSONArrayConverter<ChatEntry>(new ChatEntry.ChatConverter()), ui);
		} catch (RestException e) {
			err.restError(e);
		}
		
	}
	
	@Override
	public void checkConnectionStatus() {
		checkConnectionStatus(null, 0);	//TODO implement directly
	}
	
	public void checkConnectionStatus(IModelResult<Boolean> ui,	int externalTag) {
		try {
			startAsyncTask(Tasks.CHECK_SERVER, pull.pendingServerStatus(server), null, ui);
		} catch (RestException e) {
			err.restError(e);
		}
		
	}
	
	@Deprecated
	public void getMapNodes(IModelResult<List<MapNode>> ui, int externalTag) {
		if (!loggedIn) {
			err.notLoggedIn();
			return;
		}
		try {
			startAsyncTask(Tasks.MAP_NODES, pull.pendingMapNodes(), new StringedJSONArrayConverter<MapNode>(new MapNode.NodeConverter()), ui);
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	int getNewTaskId() {
		return --taskID;
	}
	
	private <T, V> int startAsyncTask(Tasks internalTask, PendingRequest payload, IConverter<String, V> converter, IModelResult<T> ui) {
		AsyncRequest<V> r = new AsyncRequest<V>(this, --taskID, converter);
		callbacks.put(taskID, new Task<T, V>(internalTask, r, ui));
		r.execute(payload);
		
		return taskID;
	}
	
	@Override
	public boolean isLoggedIn() {
		return loggedIn;
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
	
	@Deprecated
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
			final Task<Boolean, List<Integer>> t = (Task<Boolean, List<Integer>>) callbacks.get(internalTag);
			
			boolean success = false;
			try {
				if (!t.task.isSuccessfull())
					throw t.task.getException();
			
				chatrooms = Chatroom.getChatrooms(t.task.get(), this);
				success = true;
				logInSuccessfull();
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
			
			t.callback(success, internalTag);
			
			break;
		case CHAT_REFRESH:
		case CHAT_DOWNLOAD:
			final QuickTask<List<ChatEntry>> t2 = (QuickTask<List<ChatEntry>>) callbacks.get(internalTag);
			if (t2.task.isSuccessfull()){
				t2.callback(internalTag);
			} else
				err.asyncTaskResponseError(t2.task.getException());
			
			break;
		case CHECK_SERVER:
			final Task<Boolean, String> t3 = callbacks.get(internalTag);
			if (task.isSuccessfull()) {
				loggedIn = (task.getResponseCode() >= 200 && task.getResponseCode() < 300);
				connectionStatusChange();
			} else
				err.asyncTaskResponseError(t3.task.getException());
			
			t3.callback(loggedIn, internalTag);
			
			break;
		case MAP_NODES:
			final QuickTask<List<MapNode>> t4 = (QuickTask<List<MapNode>>) callbacks.get(internalTag);
			
			if (t4.task.isSuccessfull()) {
				t4.callback(internalTag);
			} else
				err.asyncTaskResponseError(t4.task.getException());
			
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
	static class Task<T, V> {
		IModelResult<T> externalCallback;
		AsyncRequest<V> task;
		Tasks type;
		
		
		public Task(Tasks type, AsyncRequest<V> task, IModelResult<T> externalCallback) {
			this.externalCallback = externalCallback;
			this.task = task;
			this.type = type;
		}
		
		public void callback(T result, int taskId) {
			if (externalCallback != null)
				externalCallback.onModelFinished(taskId, result);
		}
	}
	
	private class QuickTask<T> extends Task<T, T> {
		public QuickTask(Tasks type, AsyncRequest<T> task, IModelResult<T> externalCallback) {
			super(type, task, externalCallback);
		}
		
		public void callback(int taskId) {
			if (externalCallback != null)
				try {
					callback(task.get(), taskId);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
