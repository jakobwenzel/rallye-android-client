package de.stadtrallye.rallyesoft.util;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;

public class JSONArray<IN, T> extends org.json.JSONArray implements Iterable<T> {
	
	private static final ErrorHandling err = new ErrorHandling(JSONArray.class.getSimpleName());
	
	private Converter<IN, T> converter;

	public JSONArray(Converter<IN, T>  converter, String json) throws JSONException {
		super(json);
		
		this.converter = converter;
	}
	
	@Deprecated
	public JSONArray(Converter<IN,T> converter, org.json.JSONArray arr) throws JSONException {
		this(converter, arr.toString());
	}

	@Override
	public Iterator<T> iterator() {
		return new JSONIterator();
	}
	
	private class JSONIterator implements Iterator<T> {
		
		private int i = 0;

		@Override
		public boolean hasNext() {
			return i < JSONArray.this.length();
		}

		@Override
		public T next() {
			IN o = null;
			Object t = null;
			try {
				t = JSONArray.this.get(i++);
				o = (IN)t;
				return converter.convert(o);
			} catch (JSONException e) {
				e.printStackTrace();
				return converter.fallback();
			} catch (ClassCastException e) {
				err.jsonCastError(e);
				return converter.fallback();
			}
		}

		@Override
		public void remove() {
			throw new RuntimeException("Remove not Supported on MyJSONArray");
		}
		
	}
	
	public static <T> JSONArray<JSONObject, T> getInstance(Converter<JSONObject, T> converter, String js) {
		try {
			return new JSONArray<JSONObject, T>(converter, js);
		} catch (JSONException e) {
			err.jsonError(e);
			return null;
		}
	}

	@Deprecated
	public static <T> JSONArray<JSONObject, T> getInstance(Converter<JSONObject, T> converter, org.json.JSONArray js) {
		try {
			return new JSONArray<JSONObject, T>(converter, js);
		} catch (JSONException e) {
			err.jsonError(e);
			return null;
		}
	}

	public ArrayList<T> toList() {
		ArrayList<T> list = new ArrayList<T>();
		for (T t: this)
			list.add(t);
		
		return list;
	}

	public static <T> ArrayList<T> toList(JSONConverter<T> converter, String js) {
		return getInstance(converter, js).toList();
	}

}
