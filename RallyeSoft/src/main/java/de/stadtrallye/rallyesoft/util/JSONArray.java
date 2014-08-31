/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;

/**
 * Extension to org.json.JSONArray that allows Iteration (e.g. with for(T t: jsonArray))
 * 
 * 
 * @author Ramon
 *
 * @param <T> Type to get from the Iterator
 */
public class JSONArray<T> extends org.json.JSONArray implements Iterable<T> {
	
	private static final ErrorHandling err = new ErrorHandling(JSONArray.class.getSimpleName());
	
	private JSONConverter<T> converter;

	public JSONArray(JSONConverter<T>  converter, String json) throws JSONException {
		super(json);
		
		this.converter = converter;
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
	 * @param converter an IConverter that can convert each JSONObject inside this JSONArray to T
	 * @param js the JSONArray as String (as org.json.JSONArray uses it)
	 * @return an iterable JSONArray
	 */
	public static <T> JSONArray<T> getInstance(JSONConverter<T> converter, String js) {
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

    @SuppressWarnings("unchecked")
	public <KEY, TARGET> Map<KEY, TARGET> toMap(IConverter<? super T, KEY> indexer, IConverter<T, TARGET> compressor) {
		if (compressor == null)
			compressor = new IConverter<T, TARGET>() {
				@Override
				public TARGET convert(T input) {
					return (TARGET) input;
				}
			};

		HashMap<KEY, TARGET> map = new HashMap<KEY, TARGET>();
		for (T v: this)
			map.put(indexer.convert(v), compressor.convert(v));
		
		return map;
	}

	public static <T> ArrayList<T> toList(JSONConverter<T> converter, String js) {
		return getInstance(converter, js).toList();
	}

}
