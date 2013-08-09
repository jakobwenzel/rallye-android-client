package de.stadtrallye.rallyesoft.exceptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;

import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;

import android.util.Log;

public class ErrorHandling {
	
	private String where;

	public ErrorHandling(String where) {
		this.where = where;
	}
	
	public HttpRequestException JSONDuringRequestCreationError(JSONException e, URL url) {
		Log.e(where, "JSON error before connection");
		e.printStackTrace();
		return new HttpRequestException(-2,"JSON error before connection", url, e);
	}
	
	public HttpRequestException MalformedURLError(MalformedURLException e, URL base, String path) {
		Log.e(where, "Malformed URL");
		e.printStackTrace();
		return new HttpRequestException(-3,"Malformed URL: "+ path, base, e);
	}

	public void notLoggedIn() {
		Log.e(where, "Aborting, not logged in!");
	}
	
	public void loggedIn() {
		Log.e(where, "Aborting, still logged in!");
	}
	
	public void loginInvalid(ServerLogin login) {
		Log.e(where, "Invalid login: "+ login);
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

	public void dbInsertError(String string) {
		Log.e(where, "Failed to insert into DB: "+ string);
	}

	public void logoutImpossible() {
		Log.w(where, "Cannot logout: no server specified");
	}

	public void concurrentConnectionChange(String type) {
		Log.e(where, "ConnectionState is changing, refusing: "+ type);
	}

	public void connectionFailure(Exception e, IModel.ConnectionState fallbackState) {
		e.printStackTrace();
		Log.e(where,"fallback: "+ fallbackState);
	}

	public void serverNotSet() {
		Log.e(where, "Server was not set");
	}

	public void concurrentRefresh() {
		Log.e(where, "Already refreshing, cancelling...");
	}
}
