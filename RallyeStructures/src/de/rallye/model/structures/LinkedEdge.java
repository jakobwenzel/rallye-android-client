package de.rallye.model.structures;

/**
 * 
 * @author Ramon
 * @version 1.0
 */
public class LinkedEdge extends PrimitiveEdge {
	
	final public Node a;
	final public Node b;

	public LinkedEdge(Node a, Node b, PrimitiveEdge.Type type) {
		super(a.ID, b.ID, type);
		
		this.a = a;
		this.b = b;
		
		a.addEdge(this);
		b.addEdge(this);
	}
	
	public LinkedEdge(Node a, Node b, String type) {
		this(a,b, getType(type));
	}
	
	@Override
	public String toString() {
		return a +" - "+ b +" : "+ type;
	}

	public Node getOtherNode(Node node) {
		return (a==node)? b : a;
	}
}
