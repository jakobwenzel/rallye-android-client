package de.stadtrallye.rallyesoft.async;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.model.MapNode;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class PushLogin extends AsyncTask<Void, Void, int[]> {
	
	private RallyePull pull;
	private Context context;
	private String id;
	
	final private static String err = "Failed to load Nodes:: ";
	
	public PushLogin(RallyePull pull, Context context, String id) {
		this.pull = pull;
		this.context = context;
		this.id = id;
	}

	@Override
	protected int[] doInBackground(Void... params) {
		try {
			JSONArray js = pull.pushLogin();
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
			Log.e("RallyeLogin", err +e.toString());
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(int[] result) {
		Toast.makeText(context, "Logged in!", Toast.LENGTH_SHORT).show();
	}

}
