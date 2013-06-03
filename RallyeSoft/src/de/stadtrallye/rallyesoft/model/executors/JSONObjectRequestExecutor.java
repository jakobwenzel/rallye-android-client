package de.stadtrallye.rallyesoft.model.executors;

import de.stadtrallye.rallyesoft.net.Request;
import de.stadtrallye.rallyesoft.util.JSONConverter;
import de.stadtrallye.rallyesoft.util.StringedJSONObjectConverter;

public class JSONObjectRequestExecutor<T, ID> extends RequestExecutor<T, ID> {

	public JSONObjectRequestExecutor(Request req, JSONConverter<T> converter, Callback<ID> callback, ID callbackId) {
		super(req, new StringedJSONObjectConverter<T>(converter), callback, callbackId);
	}
}
