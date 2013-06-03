package de.stadtrallye.rallyesoft.model.structures;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import de.rallye.model.structures.LatLng;
import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONArray;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class Node extends de.rallye.model.structures.Node {

	public Node(int ID, String name, double lat, double lon, String description) {
		super(ID, name, lat, lon, description);
	}
	
	
	public static List<Node> translateJSON(String js) {
		return JSONArray.toList(new NodeConverter(), js);
	}
	
	public static class NodeConverter extends JSONConverter<Node> {
		
		@Override
		public Node doConvert(JSONObject o) throws JSONException {
			JSONObject p = o.getJSONObject(Node.POSITION);
			
			return new Node(
					o.getInt(Node.NODE_ID),
					o.getString(Node.NAME),
					p.getDouble(LatLng.LAT),
					p.getDouble(LatLng.LNG),
					o.getString(Node.DESCRIPTION));
		}
		
	}
	
	public static class IndexGetter implements IConverter<Node, Integer> {

		@Override
		public Integer convert(Node input) {
			return input.nodeID;
		}
		
	}

}
