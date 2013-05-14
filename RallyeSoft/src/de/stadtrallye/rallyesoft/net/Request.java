package de.stadtrallye.rallyesoft.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.util.JSONArray;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class Request {
	
	private static final boolean DEBUG = true;
	private static final String THIS = Request.class.getSimpleName();
	
	private URL url;
	private HttpURLConnection conn;
	private BufferedReader reader;
	private int code = 0;
	private Mime mime;
	private byte[] post;
	private String msg;
	
	
	public enum Mime { JSON
		{
			@Override
			public String toString() {
				return "application/JSON";
			}
		}
	};
	
	public Request(URL url) {
		this.url = url;
	}
	
	public Request putPost(byte[] post, Mime mime) {
		this.post = post;
		this.mime = mime;
		
		return this;
	}

	
	public Request putPost(JSONObject json) {
		return putPost(json.toString().getBytes(), Mime.JSON);
	}
	
	private void writePost() throws IOException {
		conn.setDoOutput(true);
		conn.addRequestProperty("Content-Type", mime.toString());
		conn.setFixedLengthStreamingMode(post.length);
		conn.getOutputStream().write(post);
	}
	
	private int prepareConnection() throws IOException, HttpRequestException {
		if (DEBUG)
			Log.i(THIS, "Connecting to: "+url.toString());
		
		
		
		conn =  (HttpURLConnection) url.openConnection();
		if (post != null) {
			if (DEBUG) {
				Log.i(THIS, "Posting: "+ new String(post));
			}
			
			writePost();
		}
		
		code = conn.getResponseCode();
		
		if (DEBUG)
			Log.d(THIS, "Received Response Code: "+code);
		
		if (code >= 200 && code < 300) {
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			if (DEBUG)
				Log.d(THIS, "Content-Length: "+ conn.getContentLength());
		} else {
			throw new HttpRequestException(code, conn.getResponseMessage(), url, null);
		}
		
		return code;
	}
	
	private void close() {
		try {
			reader.close();
			conn.disconnect();
		} catch (Exception e) {}
	}
	
	public String execute() throws HttpRequestException {
		try {
			prepareConnection();
			
			String res = reader.readLine();
			
			return res;
		} catch (IOException e) {
			Log.e(THIS, "Request Failed ("+url.toString()+")", e);
			throw new HttpRequestException(code, msg, url, e);
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
			throw new HttpRequestException(code, msg, url, e);
		} finally {
			close();
		}
	}
	
	public int getResponseCode() {
		return code;
	}
	
	public JSONObject executeJSONObject() throws JSONException, HttpRequestException {
		return new JSONObject(execute());
	}
	
	public <T> JSONArray<T> executeJSONArray(JSONConverter<T> converter) throws JSONException, HttpRequestException {
		return new JSONArray<T>(converter, execute());
	}
}
