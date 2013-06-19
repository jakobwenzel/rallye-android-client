package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.structures.Group;
import de.stadtrallye.rallyesoft.model.structures.GroupUser;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;


public interface IModel {

	enum ConnectionStatus { NoNetwork, Disconnected, Connecting, Disconnecting, Connected, Retrying }

	interface IAvailableGroupsCallback {
		void availableGroups(List<Group> groups);
	}
	
	List<? extends IChatroom> getChatrooms();
	IChatroom getChatroom(int id);
	IMap getMap();
	ServerLogin getLogin();
	GroupUser getUser();
	
	void login(ServerLogin login);
	void logout();
	void checkConfiguration();
	void getAvailableGroups(IAvailableGroupsCallback callback, String server);
	String getAvatarURL(int groupID);
	
	void addListener(IConnectionStatusListener l);
	void removeListener(IConnectionStatusListener l);
	
	ConnectionStatus getConnectionStatus();
	boolean isDisconnected();

	boolean isConnectionChanging();
	boolean isConnected();
	
	
	void onDestroy();
	void onStop();
}
