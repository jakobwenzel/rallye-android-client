package de.stadtrallye.rallyesoft.util;

import java.util.Map;

/**
 * Read a String in as JSON Array, generate a List<T> from it, put all entries in a Map<K, V> using the compressor to turn T in V and the indexer to generate K from T
 * @param <K> the target key type
 * @param <T> the temporary value type
 * @param <V> the target value type
 */
public class StringedJSONArrayToMapConverter<K, T, V> implements IConverter<String, Map<K, V>> {

	private final IConverter<? super T, K> indexer;
	private final JSONConverter<T> converter;
	private final IConverter<T, V> compressor;

	public StringedJSONArrayToMapConverter(JSONConverter<T> converter, IConverter<? super T, K> indexer, IConverter<T, V> compressor) {
		this.converter = converter;
		this.indexer = indexer;
		this.compressor = compressor;
	}

	@Override
	public Map<K, V> convert(String input) {
		return JSONArray.getInstance(converter, input).toMap(indexer, compressor);
	}
}
