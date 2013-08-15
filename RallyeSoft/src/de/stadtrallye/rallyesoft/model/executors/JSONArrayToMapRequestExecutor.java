package de.stadtrallye.rallyesoft.model.executors;

import java.util.Map;

import de.stadtrallye.rallyesoft.net.Request;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayToMapConverter;
import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONConverter;

/**
 * Read a String in as JSON Array, generate a List<T> from it, put all entries in a Map<K, V> using the compressor to turn T in V and the indexer to generate K from T
 * @param <K> the target key type
 * @param <T> the temporary value type
 * @param <V> the target value type
 */
public class JSONArrayToMapRequestExecutor<K, T, V, ID> extends RequestExecutor<Map<K, V>, ID> {

	public JSONArrayToMapRequestExecutor(Request req, JSONConverter<T> converter, IConverter<? super T, K> indexer, IConverter<T, V> compressor, Callback<ID> callback, ID callbackId) {
		super(req, new StringedJSONArrayToMapConverter<K, T, V>(converter, indexer, compressor), callback, callbackId);
	}

}
