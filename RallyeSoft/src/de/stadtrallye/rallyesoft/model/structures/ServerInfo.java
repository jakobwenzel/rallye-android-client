package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.util.JSONConverter;

/**
 * Created by Ramon on 20.06.13.
 */
public class ServerInfo extends de.rallye.model.structures.ServerInfo {

	public ServerInfo(String name, String description) {
		super(name, description);
	}

	public static class Converter extends JSONConverter<ServerInfo> {
		@Override
		public ServerInfo doConvert(JSONObject o) throws JSONException {
			return new ServerInfo(o.getString(NAME), o.getString(DESCRIPTION));
		}
	}
}
