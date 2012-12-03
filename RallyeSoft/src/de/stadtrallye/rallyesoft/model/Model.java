package de.stadtrallye.rallyesoft.model;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;
import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.async.IAsyncFinished;
import de.stadtrallye.rallyesoft.async.UniPush;
import de.stadtrallye.rallyesoft.communications.Pull.PendingRequest;
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
	
	final private String LOGGED_IN = "loggedIn";
	final private String SERVER = "server";
	final private String GROUP = "group";
	final private String PASSWORD = "password";
	
	final private static int TASK_LOGIN = 100001;
	final private static int TASK_CHAT_REFRESH = 100002;

	private SharedPreferences pref;
	private RallyePull pull;
	private Context context;
	private SparseArray<Task<? extends Object>> callbacks;
	private String server;
	private int group;
	private String password;
	
	public Model(Context context, SharedPreferences pref) {
		this.pref = pref;
		this.context = context;
		pull = new RallyePull(pref, context);
		callbacks = new SparseArray<Task<? extends Object>>();
	}
	
	public void logout(IModelResult<Boolean> ui, int tag) {
		try {
			new UniPush(new Redirect<Boolean>(ui, true), tag).execute(pull.pendingLogout());
			pref.edit().putBoolean(LOGGED_IN, false).commit();
		} catch (RestException e) {
			Log.e("Model", e.toString());
		}
	}
	
	public void login(IModelResult<Boolean> ui, int tag, String server, int group, String password) {
		
		this.server = server;
		this.group = group;
		this.password = password;
		
			try {
				UniPush p = new UniPush(this, TASK_LOGIN);
				callbacks.put(TASK_LOGIN, new Task<Boolean>(ui, tag, p));
				p.execute(pull.pendingLogin(context, server, group, password));
			} catch (RestException e) {
				Log.e("Model", "invalid Rest URL", e);
			}
	}
	
	public void refreshSimpleChat(IModelResult<JSONArray> ui, int tag) {
		try {
			 //TODO: select chatroom
			UniPush p = new UniPush(this, TASK_CHAT_REFRESH);
			callbacks.put(TASK_CHAT_REFRESH, new Task<JSONArray>(ui, tag, p));
			p.execute(pull.pendingChatRefresh(2, 0));
		} catch (RestException e) {
			Log.e("Model", "invalid Rest URL", e);
		}
	}
	
	public boolean isLoggedIn() {
		return pref.getBoolean(LOGGED_IN, false);
	}
	
	public String getServer() {
		return pref.getString(SERVER, Config.server);
	}
	
	public int getGroup() {
		return pref.getInt(GROUP, Config.group);
	}
	
	public String getPassword() {
		return pref.getString(PASSWORD, Config.password);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onAsyncFinished(int tag, UniPush task) {
		switch (tag) {
		case TASK_LOGIN:
			boolean success = false;
			try {
				JSONArray js = new JSONArray(task.get());
				if (js == null)
					throw new JSONException("null");
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
			
			SharedPreferences.Editor edit = pref.edit();
			edit.putBoolean(LOGGED_IN, success);
			if (success) {
				edit.putString(SERVER, server);
				edit.putInt(GROUP, group);
				edit.putString(PASSWORD, password);
			}
			edit.commit();
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
		}
		callbacks.remove(tag);
	}
	
	public void onDestroy() {
		for (int i = callbacks.size()-1; i>=0; --i) {
			callbacks.valueAt(i).task.cancel(true);
		}
	}
	
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


}
