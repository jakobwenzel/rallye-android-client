package de.stadtrallye.rallyesoft.model.executors;

import java.util.List;

import de.stadtrallye.rallyesoft.net.Request;
import de.stadtrallye.rallyesoft.util.JSONConverter;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayConverter;

public class JSONArrayRequestExecutor<T, ID> extends RequestExecutor<List<T>, ID> {

	public JSONArrayRequestExecutor(Request req, JSONConverter<T> converter, Callback<ID> callback, ID callbackId) {
		super(req, new StringedJSONArrayConverter<T>(converter), callback, callbackId);
	}
}
