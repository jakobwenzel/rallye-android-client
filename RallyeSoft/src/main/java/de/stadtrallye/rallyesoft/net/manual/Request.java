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

package de.stadtrallye.rallyesoft.net.manual;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.util.JSONArray;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class Request {
	
	private static final boolean DEBUG = true;
	private static final String THIS = Request.class.getSimpleName();
	
	public enum RequestType { GET, POST, PUT, DELETE }
	
	private final URL url;
	private HttpURLConnection conn;
	private BufferedReader reader;
	private int code = 0;
	private Mime mime;
	private byte[] post;
	private String msg;
	private RequestType requestType;
	
	
	public enum Mime { JSON
		{
			@Override
			public String toString() {
				return "application/JSON";
			}
		}
	}
	
	public Request(URL url) {
		this(url, RequestType.GET);
	}
	
	public Request(URL url, RequestType requestType) {
		this.url = url;
		this.requestType = requestType;
	}
	
	Request putPost(byte[] post, Mime mime) {
		this.post = post;
		this.mime = mime;
		
		return this;
	}

	public void setRequestType(RequestType requestType) {
		this.requestType = requestType;
	}
	
	public Request putPost(JSONObject json) {
		return putPost(json.toString().getBytes(), Mime.JSON);
	}
	
	private void writePost() throws IOException {
		conn.setDoOutput(true);
		conn.addRequestProperty(Std.CONTENT_TYPE, mime.toString());
//		conn.setFixedLengthStreamingMode(post.length);
		conn.getOutputStream().write(post);
	}
	
	private void prepareConnection() throws IOException, HttpRequestException {
		if (DEBUG)
			Log.i(THIS, "Connecting to: "+ url.toString());
		
		
		conn =  (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(requestType.toString());
		
		if (post != null) {
			if (DEBUG) {
				Log.i(THIS, requestType.toString() +": "+ new String(post));
			}
			
			writePost();
		}
		
		code = conn.getResponseCode();
		msg = conn.getResponseMessage();
		
		if (DEBUG)
			Log.i(THIS, "Received Response Code: "+code);
		
		if (code >= 200 && code < 300) {
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			if (DEBUG)
				Log.d(THIS, "Content-Length: "+ conn.getContentLength());
		} else {
			throw new HttpRequestException(code, conn.getResponseMessage(), url, this, null);
		}
	}
	
	private void close() {
		try {
			reader.close();
			conn.disconnect();
		} catch (NullPointerException e) {
			Log.w(THIS, "HTTPConnection was NULL during close()");
		} catch (Exception e) {
			Log.e(THIS, "HTTPConnection failed to disconnect", e);
		}
	}
	
	public String execute() throws HttpRequestException {
		try {
			prepareConnection();
			
			return reader.readLine();
		} catch (IOException e) {
			Log.e(THIS, "Request Failed ("+url.toString()+")", e);
			throw new HttpRequestException(code, msg, url, this, e);
		} finally {
			close();
		}
	}
	
	public List<String> executeMultipleLines() throws HttpRequestException {
		try {
			prepareConnection();
			
			List<String> res = new ArrayList<String>();
			String line;
			
			while((line = reader.readLine()) != null) {
				res.add(line);
			}
			return res;
		} catch (IOException e) {
			Log.e(THIS, "Request Failed ("+url.toString()+")", e);
			throw new HttpRequestException(code, msg, url, this, e);
		} finally {
			close();
		}
	}
	
	public int getResponseCode() {
		return code;
	}
	
	public <T> T executeJSONObject(JSONConverter<T> converter) throws JSONException, HttpRequestException {
		return converter.convert(new JSONObject(execute()));
	}
	
	public <T> List<T> executeJSONArray(JSONConverter<T> converter) throws JSONException, HttpRequestException {
		return new JSONArray<T>(converter, execute()).toList();
	}

    @Override
    public String toString() {
        return "Method: "+ requestType+ "\n"+
                "Target: "+ url +"\n"+
                "Mime: "+ mime +"\n"+
                "Post: "+ Arrays.toString(post) +"\n"+
                "HTTP Code: "+ code +"\n"+
                "MSG: "+ msg;
    }
}
