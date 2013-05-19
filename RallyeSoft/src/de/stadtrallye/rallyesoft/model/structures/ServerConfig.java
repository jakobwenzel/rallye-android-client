package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import de.stadtrallye.rallyesoft.util.StringedJSONObjectConverter;

public class ServerConfig extends de.rallye.model.structures.ServerConfig {
	
	public ServerConfig(String name, double lat, double lon, int rounds,int roundTime, int startTime) {
		super(name, lat, lon, rounds, roundTime, startTime);
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
}
