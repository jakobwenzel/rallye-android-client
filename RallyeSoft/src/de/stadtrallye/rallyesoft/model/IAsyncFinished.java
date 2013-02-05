package de.stadtrallye.rallyesoft.model;

import de.stadtrallye.rallyesoft.model.comm.AsyncRequest;


public interface IAsyncFinished {

	public void onAsyncFinished(AsyncRequest task, boolean success);
}
