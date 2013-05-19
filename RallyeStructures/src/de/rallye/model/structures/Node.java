package de.rallye.model.structures;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Ramon
 * @version 1.1
 */
@XmlRootElement
public class Node {
	final public int ID;
	final public String name;
	final public LatLng position;
	final public String description;
	final private ArrayList<Edge> edges = new ArrayList<Edge>();

	public Node(int ID, String name, double lat, double lon, String description) {
		this.ID = ID;
		this.name = name;
		this.description = description;
		this.position = new LatLng(lat, lon);
	}
	
	public void addEdge(Edge edge) {
		edges.add(edge);
	}
	
	public List<Edge> getEdges() {
		return edges;
	}
	
	
	@Override
	public String toString() {
		return name +" ( "+position.latitude+" , "+position.longitude+" )";
	}
}
