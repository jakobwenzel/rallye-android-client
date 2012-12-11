package de.stadtrallye.rallyesoft.model;

public class MapNode {
	public int ID;
	public String name;
	public int lat;
	public int lon;
	public String description;

	public MapNode(int ID, String name, double lat, double lon, String description) {
		this.ID = ID;
		this.name = name;
		this.lat = (int) (lat * 1000000);
		this.lon = (int) (lon * 1000000);
		this.description = description;
	}
	
	
	@Override
	public String toString() {
		return name +" ( "+lat+" , "+lon+" )";
	}
}
