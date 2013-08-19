package de.stadtrallye.rallyesoft.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Special IConverter to convert already interpreted JSONObject's to T
 * @author Ramon
 *
 * @param <T> Target type
 */
public abstract class JSONConverter<T> implements IConverter<JSONObject, T> {
	
	public T fallback() {
		return null;
	}
	
	@Override
	public T convert(JSONObject input) {
		try {
			return doConvert(input);
		} catch (JSONException e) {
			Log.w("JSONConverter", "falling back, conversion failed", e);
			return fallback();
		}
	}
	
	public abstract T doConvert(JSONObject o) throws JSONException;
}
