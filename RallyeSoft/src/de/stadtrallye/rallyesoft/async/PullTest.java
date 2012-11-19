package de.stadtrallye.rallyesoft.async;

import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.communications.Pull;
import de.stadtrallye.rallyesoft.exception.RestException;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;

public class PullTest extends AsyncTask<Void, Void, String> {

	private Pull pull;
	private Fragment ui;
	
	public PullTest(Fragment ui, Pull pull) {
		this.ui = ui;
		this.pull = pull;
	}

	@Override
	protected String doInBackground(Void... params) {
		JSONArray js = null;
		try {
			js = pull.makeRequest("/map/getAllNodes").getJSONArray();
		} catch (RestException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			return js.getJSONObject(0).toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	@Override
	protected void onPostExecute(String res) {
		try {
			View view = ui.getView();
			
//			TextView tv = (TextView) view.findViewById(R.id.placeholder);
//			tv.setText(res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
