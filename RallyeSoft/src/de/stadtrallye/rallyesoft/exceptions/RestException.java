package de.stadtrallye.rallyesoft.exceptions;

import android.util.Log;

public class RestException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestException(String rest, Exception e) {
		super("The REST Command at "+ rest +" was not available", e);
		
		Log.e("Rest", "The REST Command at "+ rest +" was not available: "+ e.toString(), e);
	}
}
