package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import de.stadtrallye.rallyesoft.util.StringedJSONObjectConverter;

public class ServerConfig {
	
	private String name;
	private LatLng location;
	private int rounds;
	private int roundTime;
	private int startTime;
	
	public ServerConfig(String name, double lat, double lon, int rounds, int roundTime, int startTime) {
		this.name = name;
		this.location = new LatLng(lat, lon);
		this.rounds = rounds;
		this.roundTime = roundTime;
		this.startTime = startTime;
	}
	
	public LatLng getLocation() {
		return location;
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
	
	public static class ServerConfigConverter extends StringedJSONObjectConverter<ServerConfig> {
		@Override
		public ServerConfig doConvert(JSONObject o) throws JSONException {
			Log.d("ServerConfig", "Reading in "+o.toString());
			
			return new ServerConfig(o.getString("gameName"),//TODO: change gameName to name?
					o.getDouble("locLat"),//TODO: Definitely change to lat, lon as in [nodes]
					o.getDouble("locLon"),
					o.getInt("rounds"),
					o.getInt("roundTime"),
					o.getInt("gameStartTime"));
		}
	}
	
//			gameType:String: type of the game (possible types are: stadtRallye, scotlandYard
//			gameName:String: name of the game
//			locLat:Double: center Location of the map - Latitude
//			locLon:Double: center Location of the map - Longitude
//			tickets:array[
	//			bike:int: number of tickets of type "bike"
	//			foot:int: number of tickets of type "foot"
	//			tram:int: number of tickets of type "tram"
	//			bus:int: number of tickets of type "bus"
//			]
//			rounds:int: rounds to play in this game
//			roundTime:int: time between rounds in minutes
//			gameStartTime:timestamp: timestamp where the first round starts
//			freeStartPoint:boolean: true if the group can choose the startpoint, otherwise false
}
