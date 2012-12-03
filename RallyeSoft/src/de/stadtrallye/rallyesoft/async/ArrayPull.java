package de.stadtrallye.rallyesoft.async;

import org.json.JSONArray;
import org.json.JSONException;

import de.stadtrallye.rallyesoft.communications.Pull.PendingRequest;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import android.os.AsyncTask;
import android.util.Log;

public class ArrayPull extends AsyncTask<PendingRequest, Boolean, JSONArray>{

	private IAsyncFinished ui;
	private int tag;

	public ArrayPull(IAsyncFinished ui, int tag) {
		this.ui = ui;
		this.tag = tag;
	}
	
	@Override
	protected JSONArray doInBackground(PendingRequest... r) {
		JSONArray js = null;
		try {
			js = r[0].getJSONArray();
		} catch (Exception e) {
			Log.e("ArrayPull", "Request Failed!", e);
//			e.printStackTrace();
		}
		return js;
	}
	
	@Override
	protected void onPostExecute(JSONArray result) {
		super.onPostExecute(result);
		
//		ui.onAsyncFinished(tag, this);
	}
	
}
