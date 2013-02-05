package de.stadtrallye.rallyesoft.model;

public interface IConnectionStatusListener {

	public void onConnectionStatusChange(IModel.ConnectionStatus status);
	public void onConnectionFailed(Exception e, IModel.ConnectionStatus lastStatus);
}
