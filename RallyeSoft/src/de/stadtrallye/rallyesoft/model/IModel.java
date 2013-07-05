package de.stadtrallye.rallyesoft.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.GroupUser;
import de.rallye.model.structures.PictureSize;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;


public interface IModel {

	boolean isEmpty();

	enum ConnectionStatus { NoNetwork, Disconnected, Connecting, Disconnecting, Connected, Retrying }

	interface IListAvailableCallback<T> {
		void dataAvailable(List<T> data);
	}

	interface IMapAvailableCallback<K, T> {
		void dataAvailable(java.util.Map<K, T> data);
	}

	interface IObjectAvailableCallback<T> {
		void dataAvailable(T data);
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
	void getAvailableGroups(IListAvailableCallback<Group> callback);
	void getServerInfo(IObjectAvailableCallback<ServerInfo> callback);
	
	void addListener(IConnectionStatusListener l);
	void removeListener(IConnectionStatusListener l);

	void saveModel();

	void onMissingUserName(int userID);
	void onMissingGroupName(int groupID);

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
