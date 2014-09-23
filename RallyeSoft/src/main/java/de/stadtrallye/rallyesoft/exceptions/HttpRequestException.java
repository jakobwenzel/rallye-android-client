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

import java.net.URL;

import de.stadtrallye.rallyesoft.net.manual.Request;

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
