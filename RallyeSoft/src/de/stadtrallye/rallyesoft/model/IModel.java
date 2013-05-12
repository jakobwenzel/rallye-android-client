package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.structures.Login;

public interface IModel {
	
	enum ConnectionStatus { NoNetwork, Disconnected, Connecting, Disconnecting, Connected, Retrying, Unknown };
	
	List<? extends IChatroom> getChatrooms();
	IChatroom getChatroom(int id);
	IMap getMap();
	Login getLogin();
	
	void login(Login login);
	void logout();
	void checkLoginStatus();
	
	void addListener(IConnectionStatusListener l);
	void removeListener(IConnectionStatusListener l);
	
	ConnectionStatus getConnectionStatus();
	boolean isDisconnected();
	boolean isConnectionChanging();
	boolean isConnected();
	
	
	void onDestroy();
	void onStop();
}
