package de.stadtrallye.rallyesoft.model.executors;

import java.util.Map;

import de.stadtrallye.rallyesoft.net.Request;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayToMapConverter;
import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONConverter;

/**
 * Created by Ramon on 25.06.13
 */
public class JSONArrayToMapRequestExecutor<K, V, ID> extends RequestExecutor<Map<K, V>, ID> {

	public JSONArrayToMapRequestExecutor(Request req, JSONConverter<V> converter, IConverter<? super V, K> indexer, Callback<ID> callback, ID callbackId) {
		super(req, new StringedJSONArrayToMapConverter<K, V>(converter, indexer), callback, callbackId);
	}

}
