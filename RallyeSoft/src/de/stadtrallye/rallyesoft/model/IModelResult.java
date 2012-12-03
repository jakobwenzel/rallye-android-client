package de.stadtrallye.rallyesoft.model;

public interface IModelResult<T> {

	public void onModelFinished(int tag, T result);
}
