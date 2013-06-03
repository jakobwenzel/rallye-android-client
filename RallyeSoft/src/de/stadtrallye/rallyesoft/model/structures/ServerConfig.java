package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;


import de.rallye.model.structures.LatLng;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class ServerConfig extends de.rallye.model.structures.ServerConfig {
	
	public ServerConfig(String name, double lat, double lon, int rounds, int roundTime, long startTime) {
		super(name, lat, lon, rounds, roundTime, startTime);
	}

	public static class ServerConfigConverter extends JSONConverter<ServerConfig> {
		@Override
		public ServerConfig doConvert(JSONObject o) throws JSONException {
			JSONObject p = o.getJSONObject(ServerConfig.LOCATION);
			
			return new ServerConfig(o.getString(ServerConfig.NAME),
					p.getDouble(LatLng.LAT),
					p.getDouble(LatLng.LNG),
					o.getInt(ServerConfig.ROUNDS),
					o.getInt(ServerConfig.ROUND_TIME),
					o.getLong(ServerConfig.START_TIME));
		}
	}
}
