/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.model.chat;

import android.database.Cursor;

import java.util.List;

import de.rallye.model.structures.PostChat;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.IHandlerCallback;
import de.stadtrallye.rallyesoft.model.pictures.IPictureGallery;
import de.stadtrallye.rallyesoft.model.pictures.PictureManager;
import de.stadtrallye.rallyesoft.uimodel.INotificationManager;

/**
 * Chatroom
 */
public interface IChatroom {

	void editChat(ChatEntry chatEntry);

	void pushChat(ChatEntry chatEntry, INotificationManager notificationManager);

	enum ChatroomState { Ready, Refreshing }
	enum PostState { Success, Failure, Uploading }

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
	void update() throws NoServerKnownException;

	/**
	 * Clear everything and then execute {@link #update()}
	 */
	void forceRefresh() throws NoServerKnownException;

	/**
	 * Save the UI state
	 * @param lastRead the chatID of the last read chat
	 */
	void setLastReadId(int lastRead);

	/**
	 * @return the last Read saved by {@see setLastReadId} chatID
	 */
	int getLastReadId();

	/**
	 * Save the internal Chatroom state
	 * includes lastUpdateTime and lastReadID, should be called after all updates
	 */
	void save();

	/**
	 * Convenience Method to get a Cursor of all chats and read only the ones that have not been read by the user (for notifications...)
	 * @return all chat entries posted after the saved last read chatID
	 */
	List<ChatEntry> getUnreadEntries();
	
	void addListener(IChatroomListener l);
	void removeListener(IChatroomListener l);

	/**
	 * Get the current ChatroomState of this Chatroom
	 */
	ChatroomState getState();

	/**
	 * Post a new Chat to the Chatroom
	 * @param msg the Text of the new chat
	 * @param picture reference to the picture, will be marked as confirmed
	 * @return a unique id, with which to identify the status of the chat
	 */
	PostChat postChat(String msg, PictureManager.Picture picture) throws NoServerKnownException;

	/**
	 * Manually add a chat (e.g. Received via Push)
	 */
	//void pushChat(ChatEntry chatEntry);

	/**
	 * Manually edit a chat (e.g. Received via Push)
	 * @param chatEntry chatID identifies the entry to edit, everything else will be updated
	 */
//	void editChat(ChatEntry chatEntry);

	/**
	 * a Cursor for all chats of this Chatroom
	 * @return _id, Chats.KEY_MESSAGE ("message"), Chats.KEY_TIME ("timestamp"), Chats.FOREIGN_GROUP ("groupID"), Groups.KEY_NAME ("groupName"), Chats.FOREIGN_USER ("userID"), Users.KEY_NAME ("userName"), Chats.KEY_PICTURE ("pictureHash")
	 */
	Cursor getChatCursor();

	/**
	 * Get a gallery of all pictures in this chatroom
	 * @param initialPictureId the PictureID the Gallery should display after starting (not forced, only available via the Gallery)
	 * @return Gallery Model, containing all pictures in this Chatroom in order of the ChatEntries
	 */
	IPictureGallery getPictureGallery(String initialPictureId);


	/**
	 * Contains Callbacks for the 2 states a Chatroom has
	 * and a callback for when current cursors become invalid
	 */
	public interface IChatroomListener extends IHandlerCallback {

		/**
		 * Callback for changes to the Chatrooms State
		 * @param status the new ChatroomState
		 */
		public void onStateChanged(ChatroomState status);

		/**
		 * Callback after trying to post a new message
		 * @param post the handle returned by Chatroom:postChat
		 * @param state the new state of the post
		 * @param chat the complete ChatEntry as returned by the server
		 */
		public void onPostStateChange(PostChat post, PostState state, ChatEntry chat);

		/**
		 * The DB has changed and a new ChatCursor should be requested;
		 */
		public void onChatsChanged();
	}
}
