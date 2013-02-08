package de.stadtrallye.rallyesoft.model;

import java.util.List;

public interface IModel {
	
	public enum ConnectionStatus { NoNetwork, Disconnected, Connecting, Disconnecting, Connected}
	
	public List<? extends IChatroom> getChatrooms();
	public IChatroom getChatroom(int id);
	
	public void login(String server, String password, int group);
	public void logout();
	public void checkConnectionStatus();
	
	public boolean isLoggedIn();
	public String getServer();
	public int getGroupId();
	
	public void addListener(IConnectionStatusListener l);
	public void removeListener(IConnectionStatusListener l);
	public ConnectionStatus getConnectionStatus();
	
	public String getUrlFromImageId(int id, char size);
	
	public void onDestroy();
}
