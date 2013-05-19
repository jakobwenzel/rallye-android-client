package de.stadtrallye.rallyesoft.model.structures;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.util.JSONConverter;

public class Edge extends de.rallye.model.structures.Edge {

	public Edge(Node a, Node b, Type type) {
		super(a,b,type);
	}
	
	public Edge(Node a, Node b, String type) {
		super(a, b, Edge.getType(type));
	}
	
	private static Type getType(String type) {
		for (Type t: Type.values()) {
			if (t.toString().equalsIgnoreCase(type))
				return t;
		}
		return null;
	}
	
	public static class EdgeConverter extends JSONConverter<Edge> {
		
		final private Map<Integer, Node> nodes;
		
		public EdgeConverter(Map<Integer, Node> nodes) {
			this.nodes = nodes;
		}

		@Override
		public Edge doConvert(JSONObject o) throws JSONException {
			return new Edge(
					nodes.get(o.getInt("nodeA")),
					nodes.get(o.getInt("nodeB")),
					Edge.getType(o.getString("type")));
		}
		
	}
}
