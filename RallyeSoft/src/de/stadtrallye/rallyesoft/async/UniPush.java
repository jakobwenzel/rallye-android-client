package de.stadtrallye.rallyesoft.async;

import android.os.AsyncTask;
import android.util.Log;

import de.stadtrallye.rallyesoft.communications.Pull.PendingRequest;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;

/**
 * Executes a PendingRequest (->REST), holds Result as String
 * Usable for Login, Logout
 * Enhancement for other Task possible TBD
 * @author Ray
 *
 */
public class UniPush extends AsyncTask<PendingRequest, Boolean, String> {
	
	private Exception e;
	private IAsyncFinished ui;
	private int tag;
	private int responseCode;
	
	
	public UniPush(IAsyncFinished progressUi, int tag) {
		ui = progressUi;
		this.tag = tag;
	}

	@Override
	protected String doInBackground(PendingRequest... r) {
		Log.d("UniPush", "AsyncTask ("+tag+") started!");
		try {
			String res = r[0].readLine();
			responseCode = r[0].getResponseCode();
			return res;
		} catch (HttpResponseException e) {
			this.e = e;
		} catch (RestException e) {
			this.e = e;
		}
		
		return e.toString();
	}
	
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		
		Log.d("UniPush", "AsyncTask ("+tag+") finished!");
		
//		ui.setSupportProgressBarIndeterminateVisibility(false);
		ui.onAsyncFinished(tag, this);
	}
	
	public int getResponseCode() {
		return responseCode;
	}

}
