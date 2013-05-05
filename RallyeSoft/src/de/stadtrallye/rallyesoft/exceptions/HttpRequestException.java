package de.stadtrallye.rallyesoft.exceptions;

import java.io.IOException;
import java.net.URL;

public class HttpRequestException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private URL url;
	private String msg;
	private int code;
	private Exception cause;

	public HttpRequestException(int statusCode, String msg, URL url, Exception cause) {
		this.url = url;
		this.code = statusCode;
		this.msg = msg;
		this.cause = cause;
	}
	
	public URL getURL() {
		return url;
	}
	
	public int getStatusCode() {
		return code;
	}
	
	public String getMessage() {
		return msg;
	}
	
	public Exception getCause() {
		return cause;
	}
	
	@Override
	public String toString() {
		return HttpRequestException.class.getSimpleName() +":\n"+
				"Url: " +url.toString()+"\n"+
				"StatusCode: "+ code +"\n"+
				"StatusMessage: "+ msg +"\n"+
				"Root Cause: "+ cause;
	}
}
