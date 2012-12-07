package de.stadtrallye.rallyesoft.exceptions;

import org.json.JSONException;

import android.util.Log;

public class Err {

	public static RestException postError() {
		return null;
	}
	
	public static RestException JSONDuringPostError(JSONException e, String rest) {
		Log.e("RallyePull", "Logout: Unkown JSON error during POST");
		e.printStackTrace();
		return new RestException(rest, e);
	}
}
