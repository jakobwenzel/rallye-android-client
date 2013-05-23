package de.rallye.model.structures;

public class PrimitiveEdge {
	
	public enum Type { Foot, Bike, Bus, Tram };
	
	final public int nodeA;
	final public int nodeB;
	final public Type type;

	public PrimitiveEdge(int nodeA, int nodeB, Type type) {
		this.nodeA = nodeA;
		this.nodeB = nodeB;
		this.type = type;
	}
	
	public PrimitiveEdge(int nodeA, int nodeB, String type) {
		this(nodeA, nodeB, getType(type));
	}
	
	protected static Type getType(String type) {
		for (Type t: Type.values()) {
			if (t.toString().equalsIgnoreCase(type))
				return t;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return nodeA +" - "+ nodeB +" : "+ type;
	}
}
