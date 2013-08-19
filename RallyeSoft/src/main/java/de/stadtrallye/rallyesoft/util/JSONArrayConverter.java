package de.stadtrallye.rallyesoft.util;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

/**
 * Convert a existing JSONArray Entry by Entry using the supplied converter
 */
public class JSONArrayConverter<T> implements IConverter<JSONArray, List<T>> {

	private JSONConverter<T> converter;

	public JSONArrayConverter(JSONConverter<T> converter) {
		this.converter = converter;
	}

	@Override
	public List<T> convert(JSONArray jr) {
		if (jr.length() > 0) {
			try {
				List<T> res = new ArrayList<T>();
				for (int i=0; i<jr.length(); i++) {
					res.add(converter.convert(jr.getJSONObject(i)));
				}
				return res;
			} catch (JSONException e) {
				return null;
			}
		} else
			return null;
	}
}
