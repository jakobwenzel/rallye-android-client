package de.stadtrallye.rallyesoft.model;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;
import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.async.UniPush;
import de.stadtrallye.rallyesoft.communications.Pull.PendingRequest;
import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.fragments.IAsyncFinished;
import de.stadtrallye.rallyesoft.fragments.IModelFinished;

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
	
	final private int TASK_LOGIN = 5;

	private SharedPreferences pref;
	private RallyePull pull;
	private Context context;
	private HashMap<Integer, Task> callbacks;
	private String server;
	private int group;
	private String password;
	
	public Model(Context context, SharedPreferences pref) {
		this.pref = pref;
		this.context = context;
		pull = new RallyePull(pref, context);
		callbacks = new HashMap<Integer, Model.Task>();
	}
	
	public void logout(IModelFinished ui, int tag) {
		try {
			new UniPush(pull.pendingLogout(), new Redirect(ui), tag).execute();
			pref.edit().putBoolean(LOGGED_IN, false).commit();
		} catch (RestException e) {
			Log.e("Model", e.toString());
		}
	}
	
	public void login(IModelFinished ui, int tag, String server, int group, String password) {
		
		this.server = server;
		this.group = group;
		this.password = password;
		
			try {
				UniPush p = new UniPush(pull.pendingLogin(context, server, group, password), this, TASK_LOGIN);
				callbacks.put(TASK_LOGIN, new Task(ui, tag, p));
				p.execute();
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
			callbacks.get(tag).callback(success);
			break;
		}
		
	}
	
	public void onDestroy() {
		for (Task task: callbacks.values()) {
			task.task.cancel(true);
		}
	}
	
	private class Task {
		public IModelFinished ui;
		public int tag;
		public UniPush task;
		
		public Task(IModelFinished ui, int tag, UniPush task) {
			this.ui = ui;
			this.tag = tag;
			this.task = task;
		}
		
		public void callback(boolean result) {
			ui.onModelFinished(tag, result);
		}
	}
	
	private class Redirect implements IAsyncFinished {
		private IModelFinished ui;
		
		public Redirect(IModelFinished ui) {
			this.ui = ui;
		}
		
		@Override
		public void onAsyncFinished(int tag, UniPush task) {
			ui.onModelFinished(tag, true);
		}
	}
}
