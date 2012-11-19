package de.stadtrallye.rallyesoft.exception;

import android.util.Log;

public class RestException extends Exception {

	public RestException(String rest, Exception e) {
		super("The REST Command at "+ rest +" was not available", e);
		
		Log.e("Rest", "The REST Command at "+ rest +" was not available: "+ e.toString(), e);
	}
}
