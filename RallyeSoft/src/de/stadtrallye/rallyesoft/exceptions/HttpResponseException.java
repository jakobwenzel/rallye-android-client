package de.stadtrallye.rallyesoft.exceptions;

import android.util.Log;


public class HttpResponseException extends org.apache.http.client.HttpResponseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7720873277325268997L;
	private String url;

	public HttpResponseException(int statusCode, String msg, String url) {
		super(statusCode, msg);
		this.url = url;
		Log.e("HttpResponseException", toString());
	}
	
	@Override
	public String toString() {
		return url +":: "+ getStatusCode() +": "+ getMessage();
	}

}
