package de.stadtrallye.rallyesoft.util;

import org.json.JSONException;
import org.json.JSONObject;

public class StringedJSONObjectConverter<T> implements IConverter<String, T> {

	private JSONConverter<T> converter;

	public StringedJSONObjectConverter(JSONConverter<T> converter) {
		this.converter = converter;
	}
	
	@Override
	public T convert(String input) {
		try {
			return converter.convert(new JSONObject(input));
		} catch (JSONException e) {
			return converter.fallback();
		}
	}
}
