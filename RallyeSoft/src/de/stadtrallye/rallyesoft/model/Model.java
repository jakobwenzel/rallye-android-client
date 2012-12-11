package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
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
	
	final private static int TASK_LOGIN = 100001;
	final private static int TASK_CHAT_REFRESH = 100002;
	final private static int TASK_CHECK_SERVER = 100003;

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
		}
		
		pull = new RallyePull(pref.getString(SERVER, "FAIL"), gcm, context);
		
		callbacks = new SparseArray<Task<? extends Object>>();
		listeners = new ArrayList<IModelListener>();
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
				UniPush p = new UniPush(this, TASK_LOGIN);
				callbacks.put(TASK_LOGIN, new Task<Boolean>(ui, tag, p));
				p.execute(RallyePull.pendingLogin(context, server, group, password, gcm));
				
				
				this.server = server;
				this.group = group;
				this.password = password;
			} catch (RestException e) {
				Log.e("Model", "invalid Rest URL", e);
			}
	}
	
	public void refreshSimpleChat(IModelResult<JSONArray> ui, int tag, int chatroom) {
		if (!loggedIn) {
			Log.e("Model", "Aborting RefreshSimpleChat for not logged in!");
			return;
		}
		try {
			UniPush p = new UniPush(this, TASK_CHAT_REFRESH);
			callbacks.put(TASK_CHAT_REFRESH, new Task<JSONArray>(ui, tag, p));
			p.execute(pull.pendingChatRefresh(chatroom, 0));
		} catch (RestException e) {
			Log.e("Model", "invalid Rest URL", e);
		}
	}
	
	public void checkServerStatus(IModelResult<Boolean> ui,	int tag) {
		try {
			UniPush p = new UniPush(this, TASK_CHECK_SERVER);
			callbacks.put(TASK_CHECK_SERVER, new Task<Boolean>(ui, tag, p));
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

	@SuppressWarnings("unchecked")
	@Override
	public void onAsyncFinished(int tag, UniPush task) {
		switch (tag) {
		case TASK_LOGIN:
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
				//TODO: use available chatrooms
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
		case TASK_CHAT_REFRESH:
			try {
				JSONArray js = new JSONArray(task.get());
				((Task<JSONArray>) callbacks.get(tag)).callback(js);
			} catch (InterruptedException e) {
				Log.e("Model", "Unkown Exception in UniPush", e);
			} catch (JSONException e) {
				Log.e("Model", "Unkown JSONException in UniPush", e);
			} catch (ExecutionException e) {
				Log.e("Model", "Unkown Exception in UniPush", e);
			}
			break;
		case TASK_CHECK_SERVER:
			try {
				String res = task.get();
				loggedIn = (task.getResponseCode() >= 200 && task.getResponseCode() < 300);
				if (loggedIn && callbacks.get(tag).tag != 0)
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
		saveLoginDetails(server, group, password);
		
		connectionStatusChange();
	}

	private void saveLoginDetails(String server, int group, String password) {
		SharedPreferences.Editor edit = pref.edit();
//		edit.putBoolean(LOGGED_IN, success);
		edit.putString(SERVER, server);
		edit.putInt(GROUP, group);
		edit.putString(PASSWORD, password);
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
		public int tag;
		public UniPush task;
		
		public Task(IModelResult<T> ui, int tag, UniPush task) {
			this.ui = ui;
			this.tag = tag;
			this.task = task;
		}
		
		public void callback(T result) {
			ui.onModelFinished(tag, result);
		}
	}
	
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
			l.connectionStatusChange(loggedIn);
		}
	}
}
