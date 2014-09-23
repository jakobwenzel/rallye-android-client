/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
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
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.exceptions;

import android.util.Log;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import de.stadtrallye.rallyesoft.model.IModel;

public class ErrorHandling {
	
	private final String where;

	public ErrorHandling(String where) {
		this.where = where;
	}
	
	public HttpRequestException JSONDuringRequestCreationError(JSONException e, URL url) {
		Log.e(where, "JSON error before connection");
		e.printStackTrace();
		return new HttpRequestException(-2,"JSON error before connection", url, null, e);
	}
	
	public HttpRequestException MalformedURLError(MalformedURLException e, URL base, String path) {
		Log.e(where, "Malformed URL");
		e.printStackTrace();
		return new HttpRequestException(-3,"Malformed URL: "+ path, base, null, e);
	}

	public void notLoggedIn() {
		Log.e(where, "Aborting, not logged in!");
	}

	public void requestException(Exception e) {
		Log.e(where, "Failed Request", e);
	}

	public void asyncTaskResponseError(Exception found) {
		try {
			throw found;
		} catch (InterruptedException e) {
			Log.e(where, "Unkown Exception in UniPush", e);
		} catch (JSONException e) {
			Log.e(where, "Unkown JSONException in UniPush", e);
		} catch (ExecutionException e) {
			Log.e(where, "Unkown ExecutionException in UniPush", e);
		} catch (Exception e) {
			Log.e(where, "Other Unkown Exception in UniPush", e);
		}
	}

	public void jsonError(JSONException e) {
		Log.e(where, "Unkown JSONException", e);
	}

	public void jsonCastError(ClassCastException e) {
		Log.e(where, "During JSON Conversion, Object could not be casted to source class", e);
	}

	public void connectionFailure(Exception e, IModel.ConnectionState fallbackState) {
		e.printStackTrace();
		Log.e(where,"fallback: "+ fallbackState);
	}

	public void concurrentRefresh() {
		Log.e(where, "Already refreshing, cancelling...");
	}
}
