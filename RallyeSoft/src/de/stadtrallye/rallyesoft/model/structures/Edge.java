package de.stadtrallye.rallyesoft.model.structures;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.util.JSONConverter;

public class Edge {

	public enum Type { Foot, Bike, Bus, Tram };
	
	final public Node a;
	final public Node b;
	final public Type type;

	public Edge(Node a, Node b, Type type) {
		this.a = a;
		this.b = b;
		this.type = type;
		
		a.addEdge(this);
		b.addEdge(this);
	}
	
	public Edge(Node a, Node b, String type) {
		this(a, b, Edge.getType(type));
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

	public Node getOtherNode(Node node) {
		return (a==node)? b : a;
	}
}
