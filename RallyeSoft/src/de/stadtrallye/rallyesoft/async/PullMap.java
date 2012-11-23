package de.stadtrallye.rallyesoft.async;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.communications.Pull;
import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.model.MapNode;

import android.os.AsyncTask;
import android.util.Log;

public class PullMap extends AsyncTask<Void, Void, List<MapNode>> {
	
	private RallyePull pull;
	IOnTaskFinished<List<MapNode>> target;
	
	final private static String err = "Failed to load Nodes:: ";
	
	public PullMap(RallyePull pull, IOnTaskFinished<List<MapNode>> target) {
		this.pull = pull;
		this.target = target;
	}

	@Override
	protected List<MapNode> doInBackground(Void... params) {
		ArrayList<MapNode> nodes = null;
		try {
			JSONArray js = pull.pullAllNodes();
			nodes = new ArrayList<MapNode>();
			JSONObject next;
			int i = 0;
			while ((next = (JSONObject) js.opt(i)) != null)
			{
				++i;
				nodes.add(new MapNode(next.getInt("nodeID"), next.getString("name"), next.getDouble("lat"), next.getDouble("lon")));
			}
		} catch (Exception e) {
			Log.e("RallyeMap", err +e.toString());
		}
		return nodes;
	}
	
	@Override
	public void onPostExecute(List<MapNode> result) {
		if (result == null)
			return;
		target.onTaskFinished(result);
	}

}
