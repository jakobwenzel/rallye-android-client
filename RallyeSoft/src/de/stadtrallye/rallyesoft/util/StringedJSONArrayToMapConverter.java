package de.stadtrallye.rallyesoft.util;

import java.util.Map;

/**
 * Created by Ramon on 25.06.13
 */
public class StringedJSONArrayToMapConverter<K, V> implements IConverter<String, Map<K, V>> {

	private final IConverter<? super V, K> indexer;
	private final JSONConverter<V> converter;

	public StringedJSONArrayToMapConverter(JSONConverter<V> converter, IConverter<? super V, K> indexer) {
		this.converter = converter;
		this.indexer = indexer;
	}

	@Override
	public Map<K, V> convert(String input) {
		return JSONArray.getInstance(converter, input).toMap(indexer);
	}
}
