package de.stadtrallye.rallyesoft.model.structures;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONArray;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class MapNode {
	final public int ID;
	final public String name;
	final public LatLng position;
	final public String description;

	public MapNode(int ID, String name, double lat, double lon, String description) {
		this.ID = ID;
		this.name = name;
		this.description = description;
		this.position = new LatLng(lat, lon);
	}
	
	
	@Override
	public String toString() {
		return name +" ( "+position.latitude+" , "+position.longitude+" )";
	}


	public static List<MapNode> translateJSON(String js) {
		return JSONArray.toList(new NodeConverter(), js);
	}
	
	public static class NodeConverter extends JSONConverter<MapNode> {
		
		@Override
		public MapNode doConvert(JSONObject o) throws JSONException {
			return new MapNode(
					o.getInt("nodeID"),
					o.getString("name"),
					o.getDouble("lat"),
					o.getDouble("lon"),
					o.getString("description"));
		}
		
	}
	
	public static class IndexGetter implements IConverter<MapNode, Integer> {

		@Override
		public Integer convert(MapNode input) {
			return input.ID;
		}
		
	}
}
