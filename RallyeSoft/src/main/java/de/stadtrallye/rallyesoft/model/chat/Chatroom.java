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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.rallye.model.structures.SimpleChatEntry;
import de.rallye.model.structures.SimpleChatWithPictureHash;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.IPictureGallery;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.net.PictureIdResolver;
import de.stadtrallye.rallyesoft.net.retrofit.RetroAuthCommunicator;
import de.stadtrallye.rallyesoft.storage.IDbProvider;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.uimodel.INotificationManager;
import de.stadtrallye.rallyesoft.util.converters.CursorConverters;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Ramon on 22.09.2014.
 */
public class Chatroom extends de.rallye.model.structures.Chatroom implements IChatroom {

	// statics
	private final static String CLASS = Chatroom.class.getSimpleName();
	private final String THIS;


	private final IDbProvider dbProvider;
	private final RetroAuthCommunicator comm;

	private int lastReadID;
	private long lastUpdateTime;
	private int newestID = -1;
	private ChatroomState state;
	private final ReadWriteLock stateLock = new ReentrantReadWriteLock();

	private final List<IChatroomListener> listeners = new ArrayList<>();
	private final Deque<SimpleChatWithPictureHash> postQueue = new LinkedList<>();//TODO move to database

	public Chatroom(int chatroomID, String name, int lastReadID, long lastUpdateTime) throws NoServerKnownException {
		this(chatroomID, name, lastReadID, lastUpdateTime, Server.getCurrentServer().getAuthCommunicator(), Storage.getDatabaseProvider());
	}

	public Chatroom(int chatroomID, String name, int lastReadID, long lastUpdateTime, RetroAuthCommunicator communicator, IDbProvider dbProvider) {
		super(chatroomID, name);
		this.dbProvider = dbProvider;
		this.comm = communicator;
		this.lastReadID = lastReadID;
		this.lastUpdateTime = lastUpdateTime;

		THIS = CLASS + chatroomID;
	}

	@JsonCreator
	public Chatroom(@JsonProperty("chatroomID") int chatroomID, @JsonProperty("name") String name) throws NoServerKnownException {
		this(chatroomID, name, -1, 0);
	}

	@Override
	public int getID() {
		return chatroomID;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void update() throws NoServerKnownException {
		checkServerKnown();

		comm.getChatsSince(chatroomID, lastUpdateTime, new Callback<List<ChatEntry>>() {
			@Override
			public void success(List<ChatEntry> chatEntries, Response response) {//TODO use response Date to set lastUpdateTime
				if (chatEntries.size() > 0) {

					boolean isFirstRefresh = lastUpdateTime==0;
					if (isFirstRefresh)
						Log.d(THIS,"Initial Update");

					saveChats(chatEntries);
					Log.i(THIS, chatEntries.size() +" new entries saved");

					if (isFirstRefresh)
						setLastReadId(newestID);
				} else
					Log.d(THIS, "No new Entries");

				setState(ChatroomState.Ready);

				save();
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Update failed", e);
				//TODO Server.getServer().commFailed(e);
			}
		});
	}

	@Override
	public void forceRefresh() throws NoServerKnownException {
		stateLock.writeLock().lock();
		try {
			newestID = -1;
			lastUpdateTime = 0;
			lastReadID = -1;
		} finally {
			stateLock.writeLock().unlock();
		}

		getDb().delete(DatabaseHelper.Chats.TABLE, DatabaseHelper.Chatrooms.KEY_ID + "=" + chatroomID, null);
		update();
	}

	private SQLiteDatabase getDb() {
		return dbProvider.getDatabase();
	}

	@Override
	public Cursor getChatCursor() {
		Cursor c = getDb().query(DatabaseHelper.Chats.TABLE + " AS c LEFT JOIN " + DatabaseHelper.Groups.TABLE + " AS g USING(" + DatabaseHelper.Chats.FOREIGN_GROUP + ") LEFT JOIN " + DatabaseHelper.Users.TABLE + " AS u USING(" + DatabaseHelper.Chats.FOREIGN_USER + ")",
				new String[]{DatabaseHelper.Chats.KEY_ID + " AS _id", DatabaseHelper.Chats.KEY_MESSAGE, DatabaseHelper.Chats.KEY_TIME, "c." + DatabaseHelper.Chats.FOREIGN_GROUP, DatabaseHelper.Groups.KEY_NAME, DatabaseHelper.Chats.FOREIGN_USER, DatabaseHelper.Users.KEY_NAME, DatabaseHelper.Chats.KEY_PICTURE}, DatabaseHelper.Chatrooms.KEY_ID + "=" + chatroomID, null, null, null, DatabaseHelper.Chats.KEY_ID);
		return c;
	}

	private static class PictureGallery extends de.stadtrallye.rallyesoft.model.PictureGallery {

		private final int[] pictures;
		private final int initialPos;
		private final PictureIdResolver resolver;

		public PictureGallery(int initialPos, int[] pictures, PictureIdResolver resolver) {
			this.resolver = resolver;
			this.initialPos = initialPos;
			this.pictures = pictures;
		}

		@Override
		public int getInitialPosition() {
			return initialPos;
		}

		@Override
		public int getCount() {
			return pictures.length;
		}

		@Override
		public String getPictureUrl(int pos) {
			return resolver.resolvePictureID(pictures[pos], size);
		}

	}

	@Override
	public IPictureGallery getPictureGallery(int initialPictureId) {
		Cursor c = getDb().query(DatabaseHelper.Chats.TABLE, new String[]{DatabaseHelper.Chats.KEY_PICTURE}, DatabaseHelper.Chats.KEY_PICTURE + " <> 0 AND " + DatabaseHelper.Chats.FOREIGN_ROOM + " = ?", new String[]{Integer.toString(chatroomID)}, DatabaseHelper.Chats.KEY_PICTURE, null, DatabaseHelper.Chats.KEY_TIME);
		int[] pictures = new int[c.getCount()];
		int initialPos = 0;

		int picId, i = 0;
		while (c.moveToNext()) {
			picId = c.getInt(0);
			pictures[i] = picId;
			if (picId == initialPictureId) {
				initialPos = i;
			}
			i++;
		}
		c.close();

		return new PictureGallery(initialPos, pictures, Server.getCurrentServer().getPictureResolver());
	}

	@Override
	public SimpleChatEntry postChat(String msg, String pictureHash, Integer pictureID) throws NoServerKnownException {
		checkServerKnown();

		SimpleChatWithPictureHash post = queuePost(new SimpleChatWithPictureHash(msg, pictureID, pictureHash));

		return post;
	}

	private class PostChatCallback implements Callback<ChatEntry> {

		private final SimpleChatWithPictureHash post;

		public PostChatCallback(SimpleChatWithPictureHash post) {
			this.post = post;
		}

		@Override
		public void success(ChatEntry chatEntry, Response response) {
			dequeuePost(post, chatEntry);
		}

		@Override
		public void failure(RetrofitError error) {
			requeuePost(post, error);
		}
	}


	@Override
	public void setLastReadId(int lastRead) {
		stateLock.writeLock().lock();
		try {
			if (lastReadID < lastRead) {
				lastReadID = lastRead;
//			if (needPost) {
//				needPost = false;
//				server.uiHandler.post(new Runnable() {
//					@Override
//					public void run() {
//						needPost = true;
////						ChatNotificationManager.updateNotification();//TODO somehow tell our notification class about the updated lastReadID
//					}
//				});
//			}
			}
		} finally {
			stateLock.writeLock().unlock();
		}
	}

	@Override
	public int getLastReadId() {
		stateLock.readLock().lock();
		try {
			return lastReadID;
		} finally {
			stateLock.readLock().unlock();
		}
	}

	@Override
	public List<ChatEntry> getUnreadEntries() {
		Cursor cursor = getChatCursor();

		CursorConverters.ChatCursorIds c = CursorConverters.ChatCursorIds.read(cursor);

		stateLock.readLock().lock();
		try {
			CursorConverters.moveCursorToId(cursor, c.id, lastReadID);
		} finally {
			stateLock.readLock().unlock();
		}

		List<ChatEntry> res = new ArrayList<>();

		cursor.moveToNext();
		while (!cursor.isAfterLast()) {
			res.add(CursorConverters.getChatEntry(cursor,c));
			cursor.moveToNext();
		}
		return res;
	}

	@Override
	public void addListener(IChatroomListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	@Override
	public void removeListener(IChatroomListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	@Override
	public ChatroomState getState() {
		return state;
	}

	@Override
	public void save() {
		ContentValues update = new ContentValues();

		stateLock.readLock().lock();
		try {
			update.put(DatabaseHelper.Chatrooms.KEY_LAST_REFRESH, lastUpdateTime);
			update.put(DatabaseHelper.Chatrooms.KEY_LAST_READ, lastReadID);
		} finally {
			stateLock.readLock().unlock();
		}

		getDb().update(DatabaseHelper.Chatrooms.TABLE, update, DatabaseHelper.Chatrooms.KEY_ID + "=" + chatroomID, null);
	}

	private void checkServerKnown() throws NoServerKnownException {
		if (comm == null)
			throw new NoServerKnownException();
	}

	private SimpleChatWithPictureHash queuePost(SimpleChatWithPictureHash post) {
		synchronized (postQueue) {
			postQueue.add(post);
		}
		notifyPostChange(post, PostState.Uploading);

		comm.postMessage(chatroomID, post, new PostChatCallback(post));

		Log.d(THIS, "Posting new message: "+ post);

		return post;
	}

	private void dequeuePost(SimpleChatWithPictureHash post, ChatEntry entry) {
		synchronized (postQueue) {
			postQueue.remove(post);
		}

		Log.i(THIS, "Posted message: "+ post);

		saveChat(entry);

		notifyPostChange(post, PostState.Success);
	}

	private void requeuePost(SimpleChatWithPictureHash post, Exception e) {
		comm.postMessage(chatroomID, post, new PostChatCallback(post));

		Log.e(THIS, "Post failed: "+ post, e);

		notifyPostChange(post, PostState.Failure);
	}

	private void notifyPostChange(SimpleChatWithPictureHash post, PostState state) {
		notifyPostChange(post, state, null);
	}

	private void notifyPostChange(final SimpleChatWithPictureHash post, final PostState state, final ChatEntry entry) {
		synchronized (listeners) {
			Handler handler;
			for (final IChatroomListener l : listeners) {
				handler = l.getCallbackHandler();
				if (handler == null) {
					l.onPostStateChange(post, state, entry);
				} else {
					handler.post(new Runnable() {
						@Override
						public void run() {
							l.onPostStateChange(post, state, entry);
						}
					});
				}
			}
		}
	}

	private void setState(final ChatroomState newState) {
		stateLock.writeLock().lock();
		try {
			state = newState;
		} finally {
			stateLock.writeLock().unlock();
		}

		Log.i(THIS, "State: " + newState);
	}

	/**
	 * Save ChatEntries to DB
	 *
	 * @param entries All entries that have a higher chatID than this.newestID will be saved to DB
	 */
	private void saveChats(List<ChatEntry> entries) {
		//TODO check if user / group is known, otherwise request a update from server
		//KEY_ID, KEY_TIME, FOREIGN_GROUP, FOREIGN_USER, KEY_MESSAGE, KEY_PICTURE, FOREIGN_ROOM
		SQLiteStatement s = getDb().compileStatement("INSERT INTO " + DatabaseHelper.Chats.TABLE +
				" (" + DatabaseHelper.strStr(DatabaseHelper.Chats.COLS) + ") VALUES (?, ?, ?, ?, ?, ?, " + chatroomID + ")");

		int chatId;
		List<ChatEntry> update = new ArrayList<>();

		stateLock.writeLock().lock();
		try {
			ChatEntry c;
			for (Iterator<ChatEntry> i = entries.iterator(); i.hasNext(); ) {
				c = i.next();

				if (c.chatID <= newestID) { // Already seen this entry
					if (c.timestamp > lastUpdateTime) { // Entry has changed since last seen
						update.add(c);
					}
					i.remove(); // ignore
					continue;

				}

				try {
					//				Log.d(THIS, "Inserted "+c+" in Messages");

					s.bindLong(1, c.chatID);
					s.bindLong(2, c.timestamp);
					s.bindLong(3, c.groupID);
					s.bindLong(4, c.userID);
					s.bindString(5, c.message);

					if (c.pictureID != null)
						s.bindLong(6, c.pictureID);
					else
						s.bindNull(6);

					chatId = (int) s.executeInsert();

					//				Log.d(THIS, "Inserted "+c+" in Chats");

					if (chatId != c.chatID)
						throw new SQLDataException();

				} catch (Exception e) {
					Log.e(THIS, "Single Insert failed", e);
				}
			}

			if (entries.size() > 0) {
				ChatEntry last = entries.get(entries.size() - 1);

				setLast(last.timestamp, last.chatID);
			}

			Log.i(THIS, "Received " + entries.size() + " new Chats in Chatroom " + chatroomID + " since " + lastUpdateTime);

		} catch (Exception e) {
			Log.e(THIS, "All Inserts failed", e);
		} finally {
			stateLock.writeLock().unlock();
			s.close();
		}

		if (update.size() > 0) {
			Log.w(THIS, "Chat entries were changed on Server: "+ update);
			for (ChatEntry c: update) {
				editChat(c);
			}
		}

		notifyChatsChanged();
	}

	private void notifyChatsChanged() {
		synchronized (listeners) {
			Handler handler;
			for (final IChatroomListener l : listeners) {
				handler = l.getCallbackHandler();
				if (handler == null) {
					l.onChatsChanged();
				} else {
					handler.post(new Runnable() {
						@Override
						public void run() {
							l.onChatsChanged();
						}
					});
				}
			}
		}
	}

	private void saveChat(ChatEntry chat) {
		//TODO check if user / group is known, otherwise request a update from server

		ContentValues insert = new ContentValues();
		insert.put(DatabaseHelper.Chats.KEY_ID, chat.chatID);

		fillChatContentValues(insert, chat);

		getDb().insert(DatabaseHelper.Chats.TABLE, null, insert);

		setLast(chat.timestamp, chat.chatID);
	}

	@Override
	public void editChat(ChatEntry chatEntry) {
		ContentValues update = new ContentValues();

		fillChatContentValues(update, chatEntry);

		getDb().update(DatabaseHelper.Chats.TABLE, update, DatabaseHelper.Chats.KEY_ID + "=" + chatEntry.chatID, null);

//		setLast(chatEntry.timestamp, 0);

//		lookupNames(chatEntry);
//
//		List<ChatEntry> upd = new ArrayList<>();
//		upd.add(chatEntry);

//		notifyChatsEdited(upd);
		notifyChatsChanged();
	}

	@Override
	public void pushChat(ChatEntry chatEntry, INotificationManager notificationManager) {
		stateLock.readLock().lock();
		try {
			if (chatEntry.chatID <= newestID) {
				Log.w(THIS, "Received Chat via Push that was already in the DB: " + chatEntry.chatID);
				return;
			}
		} finally {
			stateLock.readLock().unlock();
		}

		saveChat(chatEntry);

//		lookupNames(chatEntry);
//
//		List<ChatEntry> upd = new ArrayList<>();
//		upd.add(chatEntry);

		Log.i(THIS, "Pushed: " + chatEntry);

//		notifyChatsAdded(upd);
		//Only show notification if chatroom is not currently visible
		synchronized (listeners) {
			if (listeners.isEmpty()) {
				if (notificationManager != null)
					notificationManager.updateChatNotification(this);
			} else {
				notifyChatsChanged();
			}
		}
	}

	private void setLast(long updateTime, int newestID) {
		stateLock.writeLock().lock();
		try {
			if (updateTime > 0) {
				if (updateTime > lastUpdateTime)
					lastUpdateTime = updateTime;
				else
					Log.e(THIS, "Outdated lastUpdateTime: old:" + lastUpdateTime + ", new: " + updateTime);
			}

			if (newestID > 0) {
				if (newestID > this.newestID)
					this.newestID = newestID;
				else
					Log.e(THIS, "Outdated newestID: old:" + this.newestID + ", new: " + newestID);
			}
		} finally {
			stateLock.writeLock().unlock();
		}
	}

	/**
	 * Helper to remove redundancy of adding all Attributes to a ContentValues Map (except chatID)
	 */
	private void fillChatContentValues(ContentValues values, ChatEntry chatEntry) {
		values.put(DatabaseHelper.Chats.KEY_MESSAGE, chatEntry.message);
		values.put(DatabaseHelper.Chats.KEY_TIME, chatEntry.timestamp);
		values.put(DatabaseHelper.Chats.KEY_PICTURE, chatEntry.pictureID);
		values.put(DatabaseHelper.Chats.FOREIGN_GROUP, chatEntry.groupID);
		values.put(DatabaseHelper.Chats.FOREIGN_USER, chatEntry.userID);
		values.put(DatabaseHelper.Chats.FOREIGN_ROOM, chatroomID);
	}

	/**
	 * Helper, write all values worth saving to a ContentValues set, that ChatManager will save to DB
	 * @param insert
	 */
	void fillRoomContentValues(ContentValues insert) {
		stateLock.readLock().lock();
		try {
			insert.put(DatabaseHelper.Chatrooms.KEY_ID, chatroomID);
			insert.put(DatabaseHelper.Chatrooms.KEY_NAME, name);
			insert.put(DatabaseHelper.Chatrooms.KEY_LAST_REFRESH, lastUpdateTime);
			insert.put(DatabaseHelper.Chatrooms.KEY_LAST_READ, lastReadID);
		} finally {
			stateLock.readLock().unlock();
		}
	}
}
