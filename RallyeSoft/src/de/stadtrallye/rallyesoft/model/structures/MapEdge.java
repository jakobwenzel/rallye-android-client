package de.stadtrallye.rallyesoft.model.structures;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.util.JSONConverter;

public class MapEdge {

	public enum Type { Foot, Bike, Bus, Tram };
	
	final public MapNode a;
	final public MapNode b;
	final public Type type;

	public MapEdge(MapNode a, MapNode b, Type type) {
		this.a = a;
		this.b = b;
		this.type = type;
	}
	
	@Override
	public String toString() {
		return a +" - "+ b +" : "+ type;
	}
	
	private static Type getType(String type) {
		for (Type t: Type.values()) {
			if (t.toString().equalsIgnoreCase(type))
				return t;
		}
		return null;
	}
	
	public static class EdgeConverter extends JSONConverter<MapEdge> {
		
		final private Map<Integer, MapNode> nodes;
		
		public EdgeConverter(Map<Integer, MapNode> nodes) {
			this.nodes = nodes;
		}

		@Override
		public MapEdge doConvert(JSONObject o) throws JSONException {
			return new MapEdge(
					nodes.get(o.getInt("nodeA")),
					nodes.get(o.getInt("nodeB")),
					getType(o.getString("type")));
		}
		
	}
}
