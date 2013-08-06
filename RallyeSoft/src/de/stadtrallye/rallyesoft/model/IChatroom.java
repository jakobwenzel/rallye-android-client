package de.stadtrallye.rallyesoft.model;

import android.database.Cursor;

import java.util.List;

import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

/**
 * Represents 1 Chatroom, belonging to a Model
 * Currently refreshes only manually, push-implementation can be external, calling addChat()
 */
public interface IChatroom {

	enum ChatroomState { Ready, Refreshing }
	enum PostState { Success, Failure, Retrying }

	/**
	 * @return get the chatroomID of this Chatroom
	 */
	int getID();

	/**
	 * @return the Chatroom name provided by the server, or "Chatroom {getID()}" as fallback
	 */
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
	 * @return the last Read saved by {@see saveCurrentState} chatID
	 */
	int getLastState();
	
	void addListener(IChatroomListener l);
	void removeListener(IChatroomListener l);

	/**
	 * Get the current ChatroomState of this Chatroom
	 */
	ChatroomState getChatStatus();

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

	/**
	 * a Cursor for all chats of this Chatroom
	 * @return _id, Chats.KEY_MESSAGE ("message"), Chats.KEY_TIME ("timestamp"), Chats.FOREIGN_GROUP ("groupID"), Groups.KEY_NAME ("groupName"), Chats.FOREIGN_USER ("userID"), Users.KEY_NAME ("userName"), Chats.KEY_PICTURE ("pictureID")
	 */
	Cursor getChatCursor();

	/**
	 * @param initialPictureId the PictureID the Gallery should display after starting (not forced, only available via the Gallery)
	 * @return Gallery Model, containing all pictures in this Chatroom in order of the ChatEntries
	 */
	IPictureGallery getPictureGallery(int initialPictureId);


	/**
	 * Contains Callbacks for the 2 states a Chatroom has
	 * and a callback for when current cursors become invalid
	 */
	public interface IChatroomListener {

		/**
		 * Callback for changes to the Chatrooms State
		 * @param status the new ChatroomState
		 */
		public void onChatStateChanged(ChatroomState status);

		/**
		 * Callback after trying to post a new message
		 * @param id the id returned by Chatroom:postChat
		 * @param chat the complete ChatEntry as returned by the server
		 */
		public void onPostStateChange(int id, PostState state, ChatEntry chat);

		/**
		 * The DB has changed and a new ChatCursor should be requested;
		 */
		public void onChatsChanged();
	}
}
