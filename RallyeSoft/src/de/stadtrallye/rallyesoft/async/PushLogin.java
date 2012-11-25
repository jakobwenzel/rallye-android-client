package de.stadtrallye.rallyesoft.async;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gcm.GCMRegistrar;

import de.stadtrallye.rallyesoft.communications.PushService;
import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.MapNode;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class PushLogin extends AsyncTask<Void, Void, int[]> {
	
	private RallyePull pull;
	private Context context;
	private SharedPreferences pref;
	private int group;
	private String pw;
	private String server;
	
	final private static String err = "Failed to load Nodes:: ";
	
	public PushLogin(RallyePull pull, Context context, SharedPreferences pref, String server, int group, String password) {
		this.pull = pull;
		this.context = context;
		this.pref = pref;
		this.group = group;
		this.pw = password;
		this.server = server;
	}

	@Override
	protected int[] doInBackground(Void... params) {
		try {
			while (!GCMRegistrar.isRegistered(context)) {
				this.wait(10);
			}
		} catch (InterruptedException e) {
			
		}
		
		if (pref.getBoolean("loggedIn", false)) {
			try {
				pull.pushLogout();
			} catch (Exception e) {
				Log.e("PushLogin", err +e.toString());
			}
		}
		
		try {
			JSONArray js = RallyePull.pushLogin(context, server, group, pw);
			if (js == null)
				return null;
			int l = js.length();
			int[] res = new int[l];
			JSONObject next;
			for (int i=0; i<l; ++i) {
				next = js.getJSONObject(i);
				res[i] = next.getInt("chatroom");
			}
			
			return res;
		} catch (Exception e) {
			Log.e("PushLogin", err +e.toString());
			return null;
		}
	}
	
	@Override
	protected void onPostExecute(int[] result) {
		if (result != null) {
			SharedPreferences.Editor edit = pref.edit();
	        edit.putString("server", server);
	        edit.putInt("group", group);
	        edit.putString("password", pw);
	        edit.putBoolean("loggedIn", true);
	        edit.commit();
	        Log.i("PushLogin", "Login successful! (server: " +server+ " group: " +group+ ")");
		}
	}

}
