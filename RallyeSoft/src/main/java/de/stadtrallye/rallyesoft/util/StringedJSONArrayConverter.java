package de.stadtrallye.rallyesoft.util;

import java.util.List;

/**
 * Converts String containing an JSON Array to a List<T> using another JSONConverter<T>
 * @author Ramon
 *
 * @param <T> Type of data in the separate fields of the JSON Array
 */
public class StringedJSONArrayConverter<T> implements IConverter<String, List<T>> {

	private JSONConverter<T> converter;

	public StringedJSONArrayConverter(JSONConverter<T> converter) {
		this.converter = converter;
	}
	
	@Override
	public List<T> convert(String input) {
		return JSONArray.toList(converter, input);
	}
}
