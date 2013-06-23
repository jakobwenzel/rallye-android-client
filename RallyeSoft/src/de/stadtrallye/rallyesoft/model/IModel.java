package de.stadtrallye.rallyesoft.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import de.rallye.model.structures.PictureSize;
import de.stadtrallye.rallyesoft.model.structures.Group;
import de.stadtrallye.rallyesoft.model.structures.GroupUser;
import de.stadtrallye.rallyesoft.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;


public interface IModel {

	boolean isEmpty();

	enum ConnectionStatus { NoNetwork, Disconnected, Connecting, Disconnecting, Connected, Retrying }

	interface IAvailableGroupsCallback {
		void availableGroups(List<Group> groups);
	}

	interface IServerInfoCallback {
		void serverInfo(ServerInfo info);
	}

	GroupUser getUser();

	List<? extends IChatroom> getChatrooms();
	IChatroom getChatroom(int id);
	IMap getMap();

	ServerLogin getLogin();

	String setServer(String server) throws MalformedURLException;

	void login(String username, int groupID, String groupPassword);
	void logout();

	void checkConfiguration();
	void getAvailableGroups(IAvailableGroupsCallback callback);
	void getServerInfo(IServerInfoCallback callback);
	
	void addListener(IConnectionStatusListener l);
	void removeListener(IConnectionStatusListener l);

	void saveModel();

	URL getPictureUploadURL(String hash);
	String getAvatarURL(int groupID);
	String getServerPictureURL();
	String getUrlFromImageId(int pictureID, PictureSize size);

	ConnectionStatus getConnectionStatus();
	boolean isConnectionChanging();
	boolean isConnected();
	boolean isDisconnected();

	void onDestroy();
}
