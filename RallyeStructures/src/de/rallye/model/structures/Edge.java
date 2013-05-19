package de.rallye.model.structures;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Ramon
 * @version 1.0
 */
@XmlRootElement
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
	
	@Override
	public String toString() {
		return a +" - "+ b +" : "+ type;
	}

	public Node getOtherNode(Node node) {
		return (a==node)? b : a;
	}
}
