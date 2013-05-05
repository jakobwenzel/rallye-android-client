package de.stadtrallye.rallyesoft.util;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class StringedJSONObjectConverter<T> implements IConverter<String, T> {

	@Override
	public T convert(String input) {
		try {
			return doConvert(new JSONObject(input));
		} catch (JSONException e) { 
			return null;
		}
	}
	
	public abstract T doConvert(JSONObject o) throws JSONException;
}
