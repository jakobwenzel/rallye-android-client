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

	enum ConnectionState { NoNetwork, Disconnected, Connecting, Disconnecting, Connected, ServerNotAvailable, InternalConnected }

	/**
	 * Get the currently logged in User
	 * @return User Object extended by the groupID
	 */
	GroupUser getUser();

	/**
	 * Get the chatrooms available to the current user
	 * @return Unmodifiable List
	 */
	List<? extends IChatroom> getChatrooms();

	/**
	 * Convenience Method
	 * Search the List of {@link #getChatrooms()} for a Chatroom with id
	 * @param id Chatroom.getID(), chatroomID as provided by server
	 * @return null if not found
	 */
	IChatroom getChatroom(int id);

	/**
	 * Get the GameMap Model (Scotland Yard)
	 */
	IMap getMap();

	/**
	 * Get the Tasks Model (Rallye)
	 */
	ITasks getTasks();

	/**
	 * All information necessary to connect to our server (with our login)
	 * For sharing and saving
	 */
	ServerLogin getLogin();

	/**
	 * Get the ServerInfo of the current Server
	 * @return null if not yet available (IModelListeners will be notified if that changes)
	 */
	ServerInfo getServerInfo();

	/**
	 * Get all groups currently in the Database
	 * @return null if not available yet
	 */
	List<Group> getAvailableGroups();

	/**
	 * Step 1:
	 * @param server valid URL, normally consisting of http/https, domain, port, path
	 * @return for convenience
	 * @throws MalformedURLException
	 */
	String setServer(String server) throws MalformedURLException;

	/**
	 * Step 3: Actually connect to the Server of Step 1
	 * @param username your username of choice (at least 3 letters)
	 * @param groupID find through {@link #refreshAvailableGroups()}
	 * @param groupPassword password needed to login to this group (ask the server admin)
	 */
	void login(String username, int groupID, String groupPassword);

	/**
	 * Disconnect from Server
	 */
	void logout();

	/**
	 * Check if the server is available again in case of temporary disconnection, due to errors like Timeouts
	 */
	void reconnect();

	/**
	 * Download available chatrooms and the newest ServerConfig
	 */
	void checkConfiguration();

	/**
	 * Get the list of all groups on the server
	 */
	void refreshAvailableGroups();
	
	void addListener(IModelListener l);
	void removeListener(IModelListener l);

	/**
	 * Write the current state to storage, so it can be recovered at a later time
	 * saves Login, ServerConfig, Chatrooms, ConnectionState
	 */
	void saveModel();

	/**
	 * Triggers an update of the internal list of all users
	 * Used for Name resolution in chat and others
	 * (Chatrooms should be notified if finished)
	 * @param userID the userId for which no name could be found in the database [UNUSED]
	 */
	void onMissingUserName(int userID);

	/**
	 * Triggers an update of the internal list of all groups
	 * Used for Name resolution in chat and others
	 * (No callback, much less likely)
	 * @param groupID the groupId for which no name could be found in the database [UNUSED]
	 */
	void onMissingGroupName(int groupID);

	/**
	 * Generate the URL to upload a new Picture
	 * @param hash some String used to identify the picture while it has not finished uploading (for example, posting a chat)
	 * @return the URL the picture can be HTTP PUT to
	 */
	URL getPictureUploadURL(String hash);

	/**
	 * The Avatar picture of groupID on the current server
	 * @param groupID existing group
	 * @return URL as String (ImageLoader only accepts Strings!)
	 */
	String getAvatarURL(int groupID);

	/**
	 * The Avatar of the current server
	 * @return URL as String (ImageLoader only accepts Strings!)
	 */
	String getServerPictureURL();

	/**
	 * Put together the URL for pictureID
	 * @param pictureID a valid pictureID (e.g. from chats)
	 * @param size The requested Size of the Picture: Thumbnail, Normal, Original
	 * @return URL as String (ImageLoader!)
	 */
	String getUrlFromImageId(int pictureID, PictureSize size);

	/**
	 * Get the current State of the Model
	 */
	ConnectionState getConnectionState();

	/**
	 * Either Disconnecting or Connecting
	 */
	boolean isConnectionChanging();

	/**
	 * Connected
	 */
	boolean isConnected();

	/**
	 * Disconnected
	 */
	boolean isDisconnected();

	/**
	 * Terminate all running Tasks and Connections currently held by Model and subsidiaries
	 */
	void onDestroy();

	/**
	 * Listener for Model State
	 * ConnectionState, ServerConfig
	 */
	public interface IModelListener {

		void onConnectionStateChange(ConnectionState newState);
		void onConnectionFailed(Exception e, ConnectionState fallbackState);

		void onServerConfigChange();

		void onServerInfoChange(ServerInfo info);

		void onAvailableGroupsChange(List<Group> groups);
	}
}
