package de.stadtrallye.rallyesoft.fragments;

import de.stadtrallye.rallyesoft.async.UniPush;

public interface IAsyncFinished {

	public void onAsyncFinished(int tag, UniPush task);
}
