package de.stadtrallye.rallyesoft.model;

import de.rallye.model.structures.PictureSize;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

public interface IChatroom {

	enum ChatStatus { Ready, Refreshing, Posting, Offline }
	
	int getID();
	String getName();

	/**
	 * Request Chats from server since last update
	 */
	void refresh();

	/**
	 * Save the UI state
	 * @param lastRead the chatID of the last read chat
	 */
	void saveCurrentState(int lastRead);

	/**
	 * @return the last Read saved by {@link saveCurrentState} chatID
	 */
	int getLastState();
	
	void addListener(IChatListener l);
	void removeListener(IChatListener l);

	ChatStatus getChatStatus();

	/**
	 * Request the Model, to callback to all IChatListeners.chatsAdded with all available chats
	 * For initializing purposes
	 */
	void provideChats(IChatListener callback);

	/**
	 * Post a new Chat to the Chatroom
	 * @param msg the Text of the new chat
	 * @param pictureID Nullable pictureID
	 */
	void postChat(String msg, Integer pictureID);

	/**
	 * Manually add a chat (e.g. Received via Push)
	 */
	void addChat(ChatEntry chatEntry);

	/**
	 * Manually edit a chat (e.g. Received via Push)
	 * @param chatEntry chatID identifies the entry to edit, everything else will be updated
	 */
	void editChat(ChatEntry chatEntry);

	/**
	 * @param initialPictureId the PictureID the Gallery should display after starting (not forced, only available via the Gallery)
	 * @return Gallery Model, containing all pictures in this Chatroom in order of the ChatEntries
	 */
	IPictureGallery getPictureGallery(int initialPictureId);
}
