package de.rallye.model.structures;

public class ServerConfig {
	
	final public String name;
	final public LatLng location;
	final public float zoomLevel = 13;
	final public int rounds;
	final public int roundTime;
	final public int startTime;
	
	public ServerConfig(String name, double lat, double lon, int rounds, int roundTime, int startTime) {
		this.name = name;
		this.location = new LatLng(lat, lon);
		this.rounds = rounds;
		this.roundTime = roundTime;
		this.startTime = startTime;
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		
		s.append("Game : ").append(name).append('\n')
		.append("Location: ").append(location.toString()).append('\n')
		.append("Round: ").append(rounds).append(" RoundTime: ").append(roundTime).append('\n')
		.append("Start: ").append(startTime);
		
		return s.toString();
	}
	
	public boolean isComplete() {
		return name != null;
	}
}
