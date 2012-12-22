package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class MapNode {
	public int ID;
	public String name;
	public int lat;
	public int lon;
	public String description;

	public MapNode(int ID, String name, double lat, double lon, String description) {
		this.ID = ID;
		this.name = name;
		this.lat = (int) (lat * 1000000);
		this.lon = (int) (lon * 1000000);
		this.description = description;
	}
	
	
	@Override
	public String toString() {
		return name +" ( "+lat+" , "+lon+" )";
	}


	public static List<MapNode> translateJSON(JSONArray js) {
		ArrayList<MapNode> nodes = new ArrayList<MapNode>();
		
		JSONObject next;
		int i = 0;
		while ((next = (JSONObject) js.opt(i)) != null)
		{
			++i;
			try {
				nodes.add(new MapNode(next.getInt("nodeID"), next.getString("name"), next.getDouble("lat"), next.getDouble("lon"), next.getString("description")));
			} catch (JSONException e) {
				Log.e("PullChat", e.toString());
				e.printStackTrace();
			}
		}
		
		return nodes;
	}
}
