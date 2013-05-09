package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.structures.Login;

public interface IModel {
	
	public enum ConnectionStatus { NoNetwork, Disconnected, Connecting, Disconnecting, Connected, Retrying, Unknown };
	
	public List<? extends IChatroom> getChatrooms();
	public IChatroom getChatroom(int id);
	
	public void login(Login login);
	public void logout();
	public void checkLoginStatus();
	
	public boolean isConnected();
//	public String getServer();
//	public int getGroupId();
	public Login getLogin();
	
	public void addListener(IConnectionStatusListener l);
	public void removeListener(IConnectionStatusListener l);
	public void addListener(IMapListener l);
	public void removeListener(IMapListener l);
	public ConnectionStatus getConnectionStatus();
	
	public void onDestroy();
	public void onStop();
	public boolean isDisconnected();
	
}
