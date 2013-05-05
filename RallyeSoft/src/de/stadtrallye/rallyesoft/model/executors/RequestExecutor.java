package de.stadtrallye.rallyesoft.model.executors;

import de.stadtrallye.rallyesoft.model.comm.Request;
import de.stadtrallye.rallyesoft.util.IConverter;

public class RequestExecutor<T, ID> extends MyRunnable<T> {
		
	protected Request r;
	protected int code;
	private IConverter<String, T> converter;
	private Callback<ID> callback;
	private ID callbackId;
	
	public RequestExecutor(Request req, IConverter<String, T> converter, Callback<ID> callback, ID callbackId) {
		this.r = req;
		this.converter = converter;
		this.callback = callback;
		this.callbackId = callbackId;
	}
	
	public interface Callback<ID> {
		void executorResult(RequestExecutor<?, ID> r, ID callbackId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T tryRun() throws Exception {
		String s = r.execute();
		code = r.getResponseCode();
		
		if (converter != null)
			return converter.convert(s);
		else
			return (T) s;
	}
	
	public int getResponseCode() {
		return code;
	}

	@Override
	protected void callback() {
		callback.executorResult(this, callbackId);
	}

}
