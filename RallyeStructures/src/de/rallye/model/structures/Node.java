package de.rallye.model.structures;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * 
 * @author Ramon
 * @version 1.1
 */
public class Node {
	
	public static final String NODE_ID = "nodeID";
	public static final String NAME = "name";
	public static final String POSITION = "position";
	public static final String DESCRIPTION = "description";
	
	final public int nodeID;
	final public String name;
	final public LatLng position;
	final public String description;
	
	@JsonIgnore final private ArrayList<LinkedEdge> edges = new ArrayList<LinkedEdge>();

	public Node(int ID, String name, double lat, double lon, String description) {
		this.nodeID = ID;
		this.name = name;
		this.description = description;
		this.position = new LatLng(lat, lon);
	}
	
	public void addEdge(LinkedEdge edge) {
		edges.add(edge);
	}
	
	public List<LinkedEdge> getEdges() {
		return edges;
	}
	
	
	@Override
	public String toString() {
		return name +" ( "+position.latitude+" , "+position.longitude+" )";
	}
}
