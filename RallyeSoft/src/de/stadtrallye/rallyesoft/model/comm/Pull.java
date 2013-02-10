package de.stadtrallye.rallyesoft.model.comm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;

/**
 * Pull represents one specific Server
 * @author Ray
 *
 */
public class Pull {
	
	private static final String THIS = Pull.class.getSimpleName();
	
	protected static boolean DEBUG = false;
	
	private String base;
	
	public enum Mime { JSON
		{
			@Override
			public String toString() {
				return "application/JSON";
			}
		}
	};
	
	public enum RequestResponse { jsArray, jsObject, String	};
	
	public void setBaseURL(String baseURL) {
		this.base = baseURL;
	}
	
	public static void enableDebugLogging() {
		DEBUG = true;
	}
	
	/**
	 * A Request that only actually connects to the Server if read from using readLine() [getJSONArray(), getJSONObject() implicit]
	 * @author Ray
	 *
	 */
	public class PendingRequest extends Request {

		private String post;
		private Mime type;
		private RequestResponse responseType;
		
		public PendingRequest(String rest) throws RestException {
			saveURL(rest);
		}
		
		public RequestResponse getResponseType() {
			return responseType;
		}

		public void setResponseType(RequestResponse responseType) {
			this.responseType = responseType;
		}

		@Override
		public boolean putPost(String post, Mime type) {
			this.post = post;
			this.type = type;
			
			return true;
		}
		
		@Override
		public String readLine() throws HttpResponseException, RestException {
			openConnection();
			if (post != null) {
				super.putPost(post, type);
			}
			return super.readLine();
		}
		
	}
	
	/**
	 * Simple Rest Request to the server of Pull-instance
	 * can post information using putPost(...)
	 * @author Ray
	 *
	 */
	public class Request {
		
		protected URL url;
		protected HttpURLConnection conn;
		private BufferedReader reader;
		private int code = 0;
		
		public Request(String rest) throws RestException {
			saveURL(rest);
			openConnection();
		}
		
		protected Request() {
			// Only for PendingRequest
		}
		
		protected void saveURL(String rest) throws RestException {
			try {
				this.url = new URL(base + rest);
			} catch (MalformedURLException e) {
				throw new RestException(rest, e);
			}
		}
		
		protected void openConnection() throws RestException {
			try {
				conn =  (HttpURLConnection) url.openConnection();
				
			} catch (IOException e) {
				throw new RestException(url.toString(), e);
			}
		}
		
		public boolean putPost(String post, Mime type) throws RestException {
			if (DEBUG)
				Log.d(THIS, "Posting: " +post);
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
				code = conn.getResponseCode();
				if (code >= 200 && code < 300) {
					if (reader == null)
						reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				} else {
					throw new HttpResponseException(code, conn.getResponseMessage(), conn.getURL().toString());
				}
				
				return reader.readLine();
			} catch (HttpResponseException e) {
				throw e;
			} catch (IOException e) {
				throw new RestException(conn.getURL().toString(), e);
			}
		}
		
		public int getResponseCode() {
			return code;
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
