package de.stadtrallye.rallyesoft.model.executors;

public abstract class MyRunnable<T> implements Runnable {
	
	private Exception exception;
	protected T res;

	@Override
	public void run() {
		try {
			res = tryRun();
		} catch (Exception e){
			exception = e;
		} finally {
			callback();
		}
	}
	
	public boolean isSuccessful() {
		return exception == null;
	}
	
	public T getResult() {
		return res;
	}

	protected abstract T tryRun() throws Exception;
	
	protected abstract void callback();

	public Exception getException() {
		return exception;
	}
}
