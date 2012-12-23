package de.stadtrallye.rallyesoft.communications;

import android.os.AsyncTask;
import android.util.Log;

import de.stadtrallye.rallyesoft.communications.Pull.PendingRequest;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.IAsyncFinished;

/**
 * Executes a PendingRequest (->REST), holds Result as String
 * Usable for Login, Logout
 * Enhancement for other Task possible TBD
 * @author Ray
 *
 */
public class AsyncRequest extends AsyncTask<PendingRequest, Boolean, String> {
	
	private static boolean DEBUG = false;
	
	private Exception e;
	private IAsyncFinished ui;
	private int tag;
	private int responseCode;
	
	
	public AsyncRequest(IAsyncFinished progressUi, int tag) {
		ui = progressUi;
		this.tag = tag;
	}
	
	public static void enableDebugLogging() {
		DEBUG = true;
	}

	@Override
	protected String doInBackground(PendingRequest... r) {
		if (DEBUG)
			Log.d("UniPush", "AsyncTask ("+tag+") started!");
		
		
		try {
			String res = r[0].readLine();
			responseCode = r[0].getResponseCode();
			r[0].close();
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
		
		if (DEBUG)
			Log.d("UniPush", "AsyncTask ("+tag+") finished!");
		
//		ui.setSupportProgressBarIndeterminateVisibility(false);
		ui.onAsyncFinished(tag, this);
	}
	
	public int getResponseCode() {
		return responseCode;
	}

}
