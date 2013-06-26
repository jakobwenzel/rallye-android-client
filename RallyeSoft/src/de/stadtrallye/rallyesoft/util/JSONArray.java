package de.stadtrallye.rallyesoft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;

/**
 * Extension to org.json.JSONArray that allows Iteration (e.g. with for(T t: jsonArray))
 * 
 * 
 * @author Ramon
 *
 * @param <IN> Type contained in this JSONArray, mostly JSONObject, but all standard JSON types are allowed
 * @param <T> Type to get from the Iterator
 */
public class JSONArray<T> extends org.json.JSONArray implements Iterable<T> {
	
	private static final ErrorHandling err = new ErrorHandling(JSONArray.class.getSimpleName());
	
	private JSONConverter<T> converter;

	public JSONArray(JSONConverter<T>  converter, String json) throws JSONException {
		super(json);
		
		this.converter = converter;
	}
	
	@Deprecated
	public JSONArray(JSONConverter<T> converter, org.json.JSONArray arr) throws JSONException {
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
			try {
				JSONObject o = JSONArray.this.getJSONObject(i++);
				return converter.convert(o);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			} catch (ClassCastException e) {
				err.jsonCastError(e);
				return null;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Remove not Supported on MyJSONArray");
		}
		
	}
	
	/**
	 * Written to block JSONException, which Java does not allow in Constructors
	 * @param converter
	 * @param js
	 * @return
	 */
	public static <T> JSONArray<T> getInstance(JSONConverter<T> converter, String js) {
		try {
			return new JSONArray<T>(converter, js);
		} catch (JSONException e) {
			err.jsonError(e);
			return null;
		}
	}

	@Deprecated
	public static <T> JSONArray<T> getInstance(JSONConverter<T> converter, org.json.JSONArray js) {
		try {
			return new JSONArray<T>(converter, js);
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
	
	public <KEY> Map<KEY, T> toMap(IConverter<? super T, KEY> indexer) {
		HashMap<KEY, T> map = new HashMap<>();
		for (T v: this)
			map.put(indexer.convert(v), v);
		
		return map;
	}

	public static <T> ArrayList<T> toList(JSONConverter<T> converter, String js) {
		return getInstance(converter, js).toList();
	}

}
