package de.stadtrallye.rallyesoft.exception;

import android.util.Log;


public class HttpResponseException extends org.apache.http.client.HttpResponseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7720873277325268997L;

	public HttpResponseException(int statusCode, String s, String t) {
		super(statusCode, s);
		Log.e("HttpRequest", t +":: "+ statusCode +": "+ s, this);
	}

}
