package de.stadtrallye.rallyesoft.model;

public class MapNode {
	public int ID;
	public String name;
	public int lat;
	public int lon;

	public MapNode(int ID, String name, double lat, double lon) {
		this.ID = ID;
		this.name = name;
		this.lat = (int) (lat * 1000000);
		this.lon = (int) (lon * 1000000);
	}
	
	
	@Override
	public String toString() {
		return name +" ( "+lat+" , "+lon+" )";
	}
}
