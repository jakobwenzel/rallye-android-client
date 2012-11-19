package de.stadtrallye.rallyesoft.communications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.exception.HttpResponseException;
import de.stadtrallye.rallyesoft.exception.RestException;

public class Pull implements Serializable {
	
	private static final long serialVersionUID = 0L;
	
	private String base;
	protected String gcm;
	
	public enum Mime {JSON{
		@Override
		public String toString() {
			return "application/JSON";
		}
		}};
	
	protected final static String GCM = "gcm";
	protected final static String TIMESTAMP = "timestamp";
	protected final static String CHATROOM = "chatroom";
	protected final static String PASSWORD = "password";
	protected final static String GROUP = "groupID";
	
	public Pull(String baseURL, String gcmID) {
		base = baseURL;
		gcm = gcmID;
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
		
		public boolean putPost(String post, Mime type) {
			byte[] bytes = post.getBytes();
			
			conn.setDoOutput(true);
			conn.addRequestProperty("Content-Type", type.toString());
			conn.setFixedLengthStreamingMode(bytes.length);
			
			try {
				conn.getOutputStream().write(bytes);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		
		public JSONArray getJSONArray() throws HttpResponseException, RestException {
			try {
				return new JSONArray(readLine());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		
		public JSONObject getJSONObject() throws HttpResponseException, RestException {
			try {
				return new JSONObject(readLine());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
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
