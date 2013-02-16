package de.stadtrallye.rallyesoft.util;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class StringedJSONObjectConverter<T> implements IConverter<String, T> {

	@Override
	public T convert(String input) {
		try {
			return doConvert(new JSONObject(input));
		} catch (JSONException e) { 
			return fallback();
		}
	}
	
	public abstract T doConvert(JSONObject o) throws JSONException;

	@Override
	public T fallback() {
		return null;
	}

}
