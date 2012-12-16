package de.stadtrallye.rallyesoft.async;

import de.stadtrallye.rallyesoft.communications.UniPush;


public interface IAsyncFinished {

	public void onAsyncFinished(int tag, UniPush task);
}
