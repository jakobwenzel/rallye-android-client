package de.stadtrallye.rallyesoft.model;

import de.stadtrallye.rallyesoft.model.comm.AsyncRequest;


public interface IAsyncFinished {

	public void onAsyncFinished(int tag, AsyncRequest task);
}
