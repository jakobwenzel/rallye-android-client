package de.stadtrallye.rallyesoft.util;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class JSONConverter<T> implements Converter<JSONObject, T> {
	
	@Override
	public T fallback() {
		return null;
	}
	
	@Override
	public T convert(JSONObject input) {
		try {
			return doConvert(input);
		} catch (JSONException e) { 
			return fallback();
		}
	}
	
	public abstract T doConvert(JSONObject o) throws JSONException;
}
