package de.stadtrallye.rallyesoft.async;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.communications.Pull;
import de.stadtrallye.rallyesoft.model.MapNode;

import android.os.AsyncTask;

public class PullMap extends AsyncTask<Void, Void, List<MapNode>> {
	
	private Pull pull;
	IOnTaskFinished<List<MapNode>> target;
	
	public PullMap(Pull pull, IOnTaskFinished<List<MapNode>> target) {
		this.pull = pull;
		this.target = target;
	}

	@Override
	protected List<MapNode> doInBackground(Void... params) {
		JSONArray js = pull.getJSONArrayFromRest("/map/get/nodes");
		ArrayList<MapNode> nodes = new ArrayList<MapNode>();
		try {
			JSONObject next;
			int i = 0;
			while ((next = (JSONObject) js.opt(i)) != null)
			{
				++i;
				nodes.add(new MapNode(next.getInt("nodeID"), next.getString("name"), next.getDouble("lat"), next.getDouble("lon")));
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return nodes;
	}
	
	@Override
	public void onPostExecute(List<MapNode> result) {
		target.onTaskFinished(result);
	}

}
