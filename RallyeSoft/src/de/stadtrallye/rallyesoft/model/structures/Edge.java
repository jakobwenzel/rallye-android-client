package de.stadtrallye.rallyesoft.model.structures;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.util.JSONConverter;

public class Edge extends de.rallye.model.structures.LinkedEdge {

	public Edge(Node a, Node b, Type type) {
		super(a,b,type);
	}
	
	public Edge(Node a, Node b, String type) {
		super(a,b,type);
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
					getType(o.getString("type")));
		}
		
	}
}
