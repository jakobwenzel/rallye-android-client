package de.stadtrallye.rallyesoft.communications;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONTokener;

import android.os.AsyncTask;
import android.util.Log;

public class Pull extends AsyncTask<String, Integer, String> {
	
	public String result;
	
	public Pull() {
		
	}
	
	public String testConnection() {
		
		
		URL url = null;
		HttpURLConnection connection = null;
		String answer = null;
		
		try {
			url = new URL("http://hajoschja.de:10101/StadtRallye/map/getAllNodes");
			connection = (HttpURLConnection) url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			answer = in.readLine();
			Log.w("pull", answer);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (connection != null) {
			
		}
		
		
		
		return answer;
	}

	@Override
	protected String doInBackground(String... params) {
		
		result = testConnection();
		Log.w("pull3", result);
		return result;
	}
	
	@Override
	protected void onPostExecute(String res) {
		Log.w("pull4", res);
		result = res;
	}
}
