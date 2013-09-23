package de.stadtrallye.rallyesoft.exceptions;

import java.net.URL;

import de.stadtrallye.rallyesoft.net.Request;

public class HttpRequestException extends Exception {

	private static final long serialVersionUID = 1L;

    private final URL url;
	private final String msg;
	private final int code;
	private final Exception cause;
	private final Request request;

	public HttpRequestException(int statusCode, String msg, URL url, Request request, Exception cause) {
		this.url = url;
		this.code = statusCode;
		this.msg = msg;
		this.cause = cause;
        this.request = request;
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
				"Url: " +url+"\n"+
				"StatusCode: "+ code +"\n"+
				"StatusMessage: "+ msg +"\n"+
				"Root Cause: "+ cause +"\n"+
                "Content: "+ request;
	}
}
