package de.stadtrallye.rallyesoft.model.structures;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONArray;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class Node {
	final public int ID;
	final public String name;
	final public LatLng position;
	final public String description;
	final private ArrayList<Edge> edges = new ArrayList<Edge>();

	public Node(int ID, String name, double lat, double lon, String description) {
		this.ID = ID;
		this.name = name;
		this.description = description;
		this.position = new LatLng(lat, lon);
	}
	
	public void addEdge(Edge edge) {
		edges.add(edge);
	}
	
	public List<Edge> getEdges() {
		return edges;
	}
	
	
	@Override
	public String toString() {
		return name +" ( "+position.latitude+" , "+position.longitude+" )";
	}


	public static List<Node> translateJSON(String js) {
		return JSONArray.toList(new NodeConverter(), js);
	}
	
	public static class NodeConverter extends JSONConverter<Node> {
		
		@Override
		public Node doConvert(JSONObject o) throws JSONException {
			return new Node(
					o.getInt("nodeID"),
					o.getString("name"),
					o.getDouble("lat"),
					o.getDouble("lon"),
					o.getString("description"));
		}
		
	}
	
	public static class IndexGetter implements IConverter<Node, Integer> {

		@Override
		public Integer convert(Node input) {
			return input.ID;
		}
		
	}
}
