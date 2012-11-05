package de.stadtrallye.rallyesoft.communications;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import de.stadtrallye.rallyesoft.error.RestNotAvailableException;

import android.os.AsyncTask;
import android.util.Log;

public class Pull {
	
	private String base;
	private boolean gzip;
	
	public Pull(String baseURL, boolean gzip) {
		base = baseURL;
		this.gzip = gzip;
	}
	
	private InputStream getInputFromRest(String rest) throws RestNotAvailableException {
		try {
			URL url = new URL(base + rest);
			return url.openConnection().getInputStream();
			
		} catch (Exception e) {
			throw new RestNotAvailableException(rest, e);
		}
	}
	
	public String readLineFromRest(String rest) throws RestNotAvailableException {
		
		try {
			return new BufferedReader(new InputStreamReader(getInputFromRest(rest))).readLine();
		} catch (IOException e) {
			throw new RestNotAvailableException(rest, e);
		}
	}
	
	public JSONArray getJSONArrayFromRest(String rest) {
		try {
			return new JSONArray(readLineFromRest(rest));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RestNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONObject getJSONObjectFromRest(String rest) {
		try {
			return new JSONObject(readLineFromRest(rest));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RestNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONArray pullAllNodes() {
		
		return getJSONArrayFromRest("/map/getAllNodes");
		
	}
}
