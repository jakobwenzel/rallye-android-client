package de.stadtrallye.rallyesoft.async;

import android.os.AsyncTask;

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
	
	
	public UniPush(IAsyncFinished progressUi, int tag) {
		ui = progressUi;
		this.tag = tag;
	}

	@Override
	protected String doInBackground(PendingRequest... r) {
		try {
			return r[0].readLine();
			
		} catch (HttpResponseException e) {
			this.e = e;
		} catch (RestException e) {
			this.e = e;
		}
		
		//Never Reached, Eclipse is stupid
		return e.toString();
	}
	
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		
//		ui.setSupportProgressBarIndeterminateVisibility(false);
		ui.onAsyncFinished(tag, this);
	}

}
