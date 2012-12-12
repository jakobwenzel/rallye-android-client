package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gcm.GCMRegistrar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.async.IAsyncFinished;
import de.stadtrallye.rallyesoft.async.UniPush;
import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.exceptions.RestException;

/**
 * My Model
 * Should be the only Class to write to Preferences
 * All Tasks should start here
 * @author Ray
 *
 */
public class Model implements IAsyncFinished {
	
	final private String SERVER = "server";
	final private String GROUP = "group";
	final private String PASSWORD = "password";
	private static final String CHATROOMS = "chatrooms";
	
	final private static int TASK_LOGIN = 100001;
	final private static int TASK_CHAT_REFRESH = 100002;
	final private static int TASK_CHECK_SERVER = 100003;
	
	
	public enum Tasks { LOGIN, CHAT_REFRESH, CHECK_SERVER };
	
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
	private ArrayList<IModelListener> listeners;
	private int[] chatrooms;
	
	public Model(Context context, SharedPreferences pref) {
		this(context, pref, false);
	}
	
	public Model(Context context, SharedPreferences pref, boolean loggedIn) {
		this.gcm = GCMRegistrar.getRegistrationId(context);
		this.pref = pref;
		this.context = context;
		this.loggedIn = loggedIn;
		
		
		server = pref.getString(SERVER, null);
		if (server == null) {
			this.loggedIn = false;
		} else {
			group = pref.getInt(GROUP, 0);
			password = pref.getString(PASSWORD, null);
			chatrooms = extractChatRooms(pref.getString(CHATROOMS, ""));
		}
		
		pull = new RallyePull(pref.getString(SERVER, "FAIL"), gcm, context);
		
		callbacks = new SparseArray<Task<? extends Object>>();
		listeners = new ArrayList<IModelListener>();
	}
	
	private int[] extractChatRooms(String string) {
		String rooms = pref.getString(CHATROOMS, "");
		String[] spl = rooms.split(";");
		ArrayList<Integer> out = new ArrayList<Integer>();
		for (int i=0; i<spl.length; i++) {
			try {
				out.add(Integer.parseInt(spl[i]));
			} catch (Exception e) {}
		}
		int[] arr = new int[out.size()];
		for (int i=0; i<out.size(); i++) {
			arr[i] = out.get(i);
		}
		return arr;
	}

	public void logout(IModelResult<Boolean> ui, int tag) {
		try {
			new UniPush(new Redirect<Boolean>(ui, true), tag).execute(pull.pendingLogout());
			loggedIn = false;
			connectionStatusChange();
		} catch (RestException e) {
			Log.e("Model", e.toString());
		}
	}
	
	public void login(IModelResult<Boolean> ui, int tag, String server, int group, String password) {
			try {
				UniPush p = new UniPush(this, --taskID);
				callbacks.put(taskID, new Task<Boolean>(ui, tag, Tasks.LOGIN, p));
				p.execute(RallyePull.pendingLogin(context, server, group, password, gcm));
				
				
				this.server = server;
				this.group = group;
				this.password = password;
			} catch (RestException e) {
				Log.e("Model", "invalid Rest URL", e);
			}
	}
	
	public void refreshSimpleChat(IModelResult<List<ChatEntry>> ui, int tag, int chatroom) {
		if (!loggedIn) {
			Log.e("Model", "Aborting RefreshSimpleChat for not logged in!");
			return;
		}
		try {
			UniPush p = new UniPush(this, --taskID);
			callbacks.put(taskID, new Task<List<ChatEntry>>(ui, tag, Tasks.CHAT_REFRESH, p));
			p.execute(pull.pendingChatRefresh(chatroom, 0));
		} catch (RestException e) {
			Log.e("Model", "invalid Rest URL", e);
		}
	}
	
	public void checkServerStatus(IModelResult<Boolean> ui,	int tag) {
		try {
			UniPush p = new UniPush(this, --taskID);
			callbacks.put(taskID, new Task<Boolean>(ui, tag, Tasks.CHECK_SERVER, p));
			p.execute(pull.pendingServerStatus(server));
		} catch (RestException e) {
			Log.e("Model", "invalid Rest URL", e);
		}
		
	}
	
	public boolean isLoggedIn() {
		return loggedIn;
	}
	
	public String getServer() {
		return pref.getString(SERVER, context.getString(R.string.default_server));
	}
	
	public int getGroup() {
		return pref.getInt(GROUP, context.getResources().getInteger(R.integer.default_group));
	}
	
	public String getPassword() {
		return pref.getString(PASSWORD, context.getString(R.string.default_password));
	}
	
	public int[] getChatRooms() {
		return chatrooms;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onAsyncFinished(int tag, UniPush task) {
		Tasks type = callbacks.get(tag).type;
		switch (type) {
		case LOGIN:
			boolean success = false;
			try {
				JSONArray js = new JSONArray(task.get());
				int l = js.length();
				int[] res = new int[l];
				JSONObject next;
				for (int i=0; i<l; ++i) {
					next = js.getJSONObject(i);
					res[i] = next.getInt("chatroom");
				}
				
				chatrooms = res;
				
				success = true;
			} catch (InterruptedException e) {
				Log.e("Model", "Unkown Exception in UniPush", e);
			} catch (JSONException e) {
				Log.e("Model", "Unkown JSONException in UniPush", e);
			} catch (ExecutionException e) {
				Log.e("Model", "Unkown Exception in UniPush", e);
			}
			
			if (success)
				logInSuccessfull();
			
			((Task<Boolean>) callbacks.get(tag)).callback(success);
			break;
		case CHAT_REFRESH:
			try {
				JSONArray js = new JSONArray(task.get());
				((Task<List<ChatEntry>>) callbacks.get(tag)).callback(ChatEntry.translateJSON(js));
			} catch (InterruptedException e) {
				Log.e("Model", "Unkown Exception in UniPush", e);
			} catch (JSONException e) {
				Log.e("Model", "Unkown JSONException in UniPush", e);
			} catch (ExecutionException e) {
				Log.e("Model", "Unkown Exception in UniPush", e);
			}
			break;
		case CHECK_SERVER:
			try {
				String res = task.get();
				loggedIn = (task.getResponseCode() >= 200 && task.getResponseCode() < 300);
				if (loggedIn && callbacks.get(tag).externalTag != 0)
					((Task<Boolean>) callbacks.get(tag)).callback(loggedIn);
				
				connectionStatusChange();
			} catch (InterruptedException e) {
				Log.e("Model", "Unkown Exception in UniPush", e);
			} catch (ExecutionException e) {
				Log.e("Model", "Unkown Exception in UniPush", e);
			} catch (Exception e) {
				Log.w("Model", "BTW: Unknwon Exception during CHECK_SERVER (to be expected if not logged in) FYI: "+ e);
			}
		}
		callbacks.remove(tag);
	}
	
	private void logInSuccessfull() {
		loggedIn = true;
		saveLoginDetails(server, group, password, chatrooms);
		
		connectionStatusChange();
	}

	private void saveLoginDetails(String server, int group, String password, int[] chatrooms) {
		SharedPreferences.Editor edit = pref.edit();
//		edit.putBoolean(LOGGED_IN, success);
		edit.putString(SERVER, server);
		edit.putInt(GROUP, group);
		edit.putString(PASSWORD, password);
		StringBuilder rooms = new StringBuilder();
		for (int i: chatrooms) {
			rooms.append(Integer.toString(i)).append(';');
		}
		edit.putString(CHATROOMS, rooms.toString());
		edit.commit();
	}

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
		public UniPush task;
		public Tasks type;
		
		public Task(IModelResult<T> ui, int externalTag, Tasks type, UniPush task) {
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
		public void onAsyncFinished(int tag, UniPush task) {
			ui.onModelFinished(tag, result);
		}
	}

	public String getImageUrl(int pictureID, char size) {
		String res = getServer() +"/pic/get/"+ pictureID +"/"+ size;
//		Log.v("model", res);
		return res;
	}
	
	public void addListener(IModelListener l) {
		listeners.add(l);
	}

	private void connectionStatusChange() {
		for(IModelListener l: listeners) {
			l.onConnectionStatusChange(loggedIn);
		}
	}
}
