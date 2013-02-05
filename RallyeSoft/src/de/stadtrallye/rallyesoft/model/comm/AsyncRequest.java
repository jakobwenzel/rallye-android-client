package de.stadtrallye.rallyesoft.model.comm;

import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;
import android.util.Log;

import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.IAsyncFinished;
import de.stadtrallye.rallyesoft.model.comm.Pull.PendingRequest;
import de.stadtrallye.rallyesoft.util.IConverter;

/**
 * Special Thread implementation
 * Executes a {@link PendingRequest} (->REST) which is returned as String
 * uses a {@link IConverter} implementation to convert from String, to T
 * @author Ray
 *
 *	@param <T> Target Type, necessary for IConverter<String, T>
 */
public class AsyncRequest<T> extends AsyncTask<PendingRequest, Void, T> {
	
	private static boolean DEBUG = false;
	
	private Exception e;
	private IAsyncFinished callback;
	private int responseCode;

	private IConverter<String, T> converter;
	
	/**
	 * Background task to execute 1 {@link PendingRequest}
	 * 
	 * <b>NOTE: </b> If T is String, converter may be <b>null<b/>
	 * @param callback will execute [@link IAsyncFinished.callback(tag, this)
	 * @param tag to uniquely identify this task
	 * @param converter Will convert the String from HTTP Response to T (to offload work of converting e.g. JSON to Objects from ui thread)
	 */
	public AsyncRequest(IAsyncFinished callback, IConverter<String, T> converter) {
		this.callback = callback;
		this.converter = converter;
	}
	
	public static void enableDebugLogging() {
		DEBUG = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected T doInBackground(PendingRequest... r) {
		if (DEBUG)
			Log.d("UniPush", "AsyncTask ("+this+") started!");
		
		
		try {
			String res = r[0].readLine();
			responseCode = r[0].getResponseCode();
			
			if (converter != null)
				return converter.convert(res);
			else
				return (T) res;
		} catch (HttpResponseException e) {
			this.e = e;
		} catch (RestException e) {
			this.e = e;
		} finally {
			r[0].close();
		}
		this.cancel(false);
		return null;
	}
	
	@Override
	protected void onPostExecute(T result) {
		super.onPostExecute(result);
		
		if (DEBUG) {
			Log.d("UniPush", "AsyncTask ("+this+") finished!");
			Log.d("UniPush", "Answer: "+ result);
		}
		
//		ui.setSupportProgressBarIndeterminateVisibility(false);
		callback.onAsyncFinished(this, isSuccessfull());
	}
	
	@Override
	protected void onCancelled(T result) {
		callback.onAsyncFinished(this, false);
	}
	
	public boolean isSuccessfull() {
		try {
			return this.get() != null;
		} catch (InterruptedException e) {
			return false;
		} catch (ExecutionException e) {
			return false;
		}
	}
	
	public Exception getException() {
		return e;
	}
	
	public int getResponseCode() {
		return responseCode;
	}

}
