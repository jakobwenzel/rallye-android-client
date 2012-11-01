package de.stadtrallye.rallyesoft.communications;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONTokener;

public class Pull {
	
	public Pull() {
		
	}
	
	public String testConnection() {
		
		
		URL url = null;
		HttpURLConnection connection = null;
		String answer = null;
		
		try {
			url = new URL("http://hajoschja.de:10101/rallye/nodes/get-all");
			connection = (HttpURLConnection) url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			answer = in.readLine();
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
}
