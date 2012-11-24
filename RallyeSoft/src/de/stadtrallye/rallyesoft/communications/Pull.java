package de.stadtrallye.rallyesoft.communications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;

public class Pull {
	
	private String base;
	
	public enum Mime {JSON{
		@Override
		public String toString() {
			return "application/JSON";
		}
		}};
	
	public Pull(String baseURL) {
		base = baseURL;
	}
	
	public Request makeRequest(String rest) throws RestException {
		return new Request(rest);
	}
	
	public class Request {
		
		private HttpURLConnection conn;
		private BufferedReader reader;
		
		public Request(String rest) throws RestException {
			try {
				URL url = new URL(base + rest);
				conn =  (HttpURLConnection) url.openConnection();
				
			} catch (IOException e) {
				throw new RestException(rest, e);
			}
		}
		
		public boolean putPost(String post, Mime type) throws RestException {
			byte[] bytes = post.getBytes();
			
			conn.setDoOutput(true);
			conn.addRequestProperty("Content-Type", type.toString());
			conn.setFixedLengthStreamingMode(bytes.length);
			
			try {
				conn.getOutputStream().write(bytes);
			} catch (IOException e) {
				throw new RestException("POST", e);
			}
			
			return true;
		}
		
		
		public String readLine() throws HttpResponseException, RestException {
			
			try {
				int code = conn.getResponseCode();
				if (code >= 200 && code < 300) {
					if (reader == null)
						reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				} else {
					throw new HttpResponseException(code, conn.getResponseMessage(), conn.getURL().toString());
				}
				
				return reader.readLine();
			} catch (IOException e) {
				throw new RestException(conn.getURL().toString(), e);
			}
		}
		
		public JSONArray getJSONArray() throws HttpResponseException, RestException, JSONException {
			return new JSONArray(readLine());
		}
		
		public JSONObject getJSONObject() throws HttpResponseException, RestException, JSONException {
			return new JSONObject(readLine());
		}
		
		public void close() {
			try {
				reader.close();
				conn.disconnect();
			} catch (Exception e) {
			}
		}
	}
}
