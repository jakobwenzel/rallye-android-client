package de.stadtrallye.rallyesoft.model;

import android.database.Cursor;

import java.util.List;

import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

public interface IChatroom {

	enum ChatroomState { Ready, Refreshing }
	enum PostState { Success, Failure, Retrying }
	
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
	
	void addListener(IChatroomListener l);
	void removeListener(IChatroomListener l);

	ChatroomState getChatStatus();

	/**
	 * Request the Model, to callback to all IChatListeners.chatsAdded with all available chats
	 * For initializing purposes
	 */
	void provideChats(IChatroomListener callback);

	/**
	 * Post a new Chat to the Chatroom
	 * @param msg the Text of the new chat
	 * @param pictureID Nullable pictureID
	 * @return a unique id, with which to identify the status of the chat
	 */
	int postChat(String msg, Integer pictureID);

	/**
	 * Manually add a chat (e.g. Received via Push)
	 */
	void addChat(ChatEntry chatEntry);

	/**
	 * Manually edit a chat (e.g. Received via Push)
	 * @param chatEntry chatID identifies the entry to edit, everything else will be updated
	 */
	void editChat(ChatEntry chatEntry);

	Cursor getChatCursor();//TODO

	/**
	 * @param initialPictureId the PictureID the Gallery should display after starting (not forced, only available via the Gallery)
	 * @return Gallery Model, containing all pictures in this Chatroom in order of the ChatEntries
	 */
	IPictureGallery getPictureGallery(int initialPictureId);


	public interface IChatroomListener {

		/**
		 * Swap each existing chat with its replacement (Identified by chatID)
		 */
		public void chatsEdited(List<ChatEntry> chats);

		/**
		 * Add these chats to the existing ones
		 */
		public void chatsAdded(List<ChatEntry> chats);

		/**
		 * Callback for IChatroom.provideChats()
		 * @param chats all available Chats
		 */
		public void chatsProvided(List<ChatEntry> chats);

		/**
		 * Callback for changes to the Chatrooms State
		 */
		public void onChatStatusChanged(ChatroomState status);

		/**
		 *
		 * @param id the id returned by Chatroom:postChat
		 */
		public void onPostStateChange(int id, PostState state, ChatEntry chatEntry);
	}
}
