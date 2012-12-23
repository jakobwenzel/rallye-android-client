package de.stadtrallye.rallyesoft.exceptions;

import java.util.concurrent.ExecutionException;

import org.json.JSONException;

import android.util.Log;

public class ErrorHandling {
	
	private String where;

	public ErrorHandling(String where) {
		this.where = where;
	}

	public RestException postError() {
		return null;
	}
	
	public RestException JSONDuringPostError(JSONException e, String rest) {
		Log.e("RallyePull", "Logout: Unkown JSON error during POST");
		e.printStackTrace();
		return new RestException(rest, e);
	}

	public void notLoggedIn() {
		Log.e(where, "Aborting, not logged in!");
	}

	public void restError(RestException e) {
		Log.e(where, "invalid Rest URL", e);
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
}
