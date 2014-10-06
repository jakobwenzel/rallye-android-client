/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *//*


package de.stadtrallye.rallyesoft.server;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.server.chat.IChatroom;
import de.stadtrallye.rallyesoft.server.converters.CursorConverters;
import de.stadtrallye.rallyesoft.server.converters.JsonConverters;
import de.stadtrallye.rallyesoft.net.PictureIdResolver;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Chatrooms;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Chats;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Groups;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Users;
import de.stadtrallye.rallyesoft.server.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.server.executors.JSONObjectRequestExecutor;
import de.stadtrallye.rallyesoft.server.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.server.structures.ChatEntry;

import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_CHATS;

public class Chatroom extends de.rallye.server.structures.Chatroom implements IChatroom, RequestExecutor.Callback<Chatroom.AdvTaskId> {

	// statics
	final private static String CLASS = Chatroom.class.getSimpleName();
	final private String THIS;
	final private static ErrorHandling err = new ErrorHandling(CLASS);

	private enum Tasks {CHAT_REFRESH, CHAT_POST}

	public static class AdvTaskId {
		final public Tasks task;
		final public int id;

		public AdvTaskId(Tasks task, int id) {
			this.task = task;
			this.id = id;
		}
	}

	// Parent Model
	final private Model server;

	// State
	private ChatroomState state;
	private long lastRefresh = 0;//Conservative: timestamp of latest ChatEntry
	private int lastId = 0;
	private int lastReadId;

	// External Ids for asynchronous tasks (Posting a chat etc.)
	private int nextTaskId = 0;

	//Listeners
	private final ArrayList<IChatroomListener> listeners = new ArrayList<IChatroomListener>();

	*/
/**
	 * @return all available Chatrooms
	 *//*

	static List<Chatroom> getChatrooms(Model server) {
		List<Chatroom> out = new ArrayList<>();

		Cursor c = server.db.query(Chatrooms.TABLE, Chatrooms.COLS, null, null, null, null, null);

		while (c.moveToNext()) {
			Chatroom room = new Chatroom(c.getInt(0), c.getString(1), server, c.getLong(2), c.getInt(3));

			if ((server.deprecatedTables & EDIT_CHATS) > 0) {
				room.resync();
				server.deprecatedTables &= ~EDIT_CHATS;
			}

			room.refresh();
			out.add(room);
		}

		Log.i(CLASS, "Restored " + out.size() + " Chatrooms");
		return out;
	}

	static void saveChatrooms(Model server, List<Chatroom> chatrooms) {
		if (chatrooms == null)
			return;

		SQLiteDatabase db = server.db;

		db.delete(Chatrooms.TABLE, null, null);

		for (Chatroom c : chatrooms) {
			ContentValues insert = new ContentValues();
			insert.put(Chatrooms.KEY_ID, c.chatroomID);
			insert.put(Chatrooms.KEY_NAME, c.name);
			insert.put(Chatrooms.KEY_LAST_REFRESH, c.lastRefresh);
			insert.put(Chatrooms.KEY_LAST_READ, c.lastReadId);
			db.insert(Chatrooms.TABLE, null, insert);
		}
	}

	public Chatroom(int id, String name, Model server) {
		this(id, name, server, 0, 0);
	}

	private Chatroom(int id, String name, Model server, long lastRefresh, int lastReadId) {
		super(id, name);
		this.server = server;
		this.lastRefresh = lastRefresh;
		this.lastReadId = lastReadId;

		THIS = CLASS + " " + id;

		Cursor c = server.db.query(Chats.TABLE, Chats.COLS, Chats.FOREIGN_ROOM + "=" + id, null, null, null, Chats.KEY_ID + " DESC", "1");

		if (c.moveToNext()) {
			this.lastId = c.getInt(0);
		}
		c.close();

		Log.i(THIS, "Chatroom restored: " + id + ", mostRecent:" + lastId + ", lastRefresh:" + lastRefresh);
		state = ChatroomState.Ready;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || ((Object) this).getClass() != o.getClass()) return false;

		Chatroom chatroom = (Chatroom) o;

		return chatroomID == chatroom.chatroomID;
	}

	// Implementation
	@Override
	public int getID() {
		return chatroomID;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return chatroomID;
	}

	@Override
	public synchronized void refresh() {
		if (!server.isConnected()) {
			err.notLoggedIn();
			return;
		}
		if (state != ChatroomState.Ready) {
			err.concurrentRefresh();
			return;
		}

		try {
			setState(ChatroomState.Refreshing);

			server.exec.execute(new JSONArrayRequestExecutor<ChatEntry, AdvTaskId>(server.factory.chatRefreshRequest(chatroomID, lastRefresh), new JsonConverters.ChatConverter(), this, new AdvTaskId(Tasks.CHAT_REFRESH, 0)));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}

	private void chatRefreshResult(RequestExecutor<List<ChatEntry>, ?> r) {
		if (r.isSuccessful()) {
			List<ChatEntry> res = r.getResult();

			if (res.size() > 0) {

				boolean isFirstRefresh = this.lastRefresh==0;

				Log.i(THIS,"Refresh first: "+isFirstRefresh);
				saveChats(res);

				if (isFirstRefresh)
					this.setLastReadId(this.lastId);
			} else
				Log.i(THIS, "No new Entries");
			setState(ChatroomState.Ready);
		} else {
			err.asyncTaskResponseError(r.getException());
			setState(ChatroomState.Ready);
			server.commError(r.getException());
		}
	}

	@Override
	public synchronized void resync() {
		lastId = 0;
		lastRefresh = 0;

		server.db.delete(Chats.TABLE, null, null);
		refresh();
	}


	@Override
	public synchronized void pushChat(ChatEntry chatEntry) {
		if (chatEntry.chatID <= this.lastId) {
			Log.w(THIS, "Received Chat via Push that was already in the DB: " + chatEntry.chatID);
			return;
		}

		saveChat(chatEntry);

//		lookupNames(chatEntry);
//
//		List<ChatEntry> upd = new ArrayList<>();
//		upd.add(chatEntry);

		Log.i(THIS, "Pushed: " + chatEntry);

//		notifyChatsAdded(upd);
		//Only show notification if chatroom is not currently visible
		if (!notifyChatsChanged())
			ChatNotificationManager.getInstance(server.context).updateNotification();
	}

	@Override
	public void editChat(ChatEntry chatEntry) {
		ContentValues update = new ContentValues();

		fillContentValues(update, chatEntry);

		server.db.update(Chats.TABLE, update, Chats.KEY_ID + "=" + chatEntry.chatID, null);

		setLast(chatEntry.timestamp, 0);

//		lookupNames(chatEntry);
//
//		List<ChatEntry> upd = new ArrayList<>();
//		upd.add(chatEntry);

//		notifyChatsEdited(upd);
		notifyChatsChanged();
	}

//	private void lookupNames(final ChatEntry chatEntry) {
//		Cursor c = server.db.query(Users.TABLE, Users.COLS, Users.KEY_ID+"="+chatEntry.userID, null, null, null, null);
//		if (c.moveToFirst()) {
//			chatEntry.setUserName(c.getString(1));
//		} else {
//			server.getAllUsers(new IModel.IMapAvailableCallback<Integer, GroupUser>() {
//				@Override
//				public void dataAvailable(Map<Integer, GroupUser> users) {
//					chatEntry.setUserName(users.get(chatEntry.userID).name);
//
//					List<ChatEntry> list = new ArrayList<>();
//					list.add(chatEntry);
//					notifyChatsEdited(list);
//				}
//			});
//		}
//		c.close();
//
//		c = server.db.query(Groups.TABLE, Groups.COLS, Groups.KEY_ID+"="+chatEntry.groupID, null, null, null, null);
//		if (c.moveToFirst()) {
//			chatEntry.setGroupName(c.getString(1));
//		} else {
//			server.getAvailableGroups(new IModel.IListAvailableCallback<Group>() {
//				@Override
//				public void dataAvailable(List<Group> groups) {
//					HashMap<Integer, Group> groupMap = new HashMap<>();
//					for (Group g: groups) {
//						groupMap.put(g.groupID, g);
//					}
//
//					chatEntry.setGroupName(groupMap.get(chatEntry.groupID).name);
//
//					List<ChatEntry> list = new ArrayList<>();
//					list.add(chatEntry);
//					notifyChatsEdited(list);
//				}
//			});
//		}
//	}

	*/
/**
	 * Helper to remove redundancy of adding all Attributes to a ContentValues Map (except chatID)
	 *//*

	private void fillContentValues(ContentValues values, ChatEntry chatEntry) {
		values.put(Chats.KEY_MESSAGE, chatEntry.message);
		values.put(Chats.KEY_TIME, chatEntry.timestamp);
		values.put(Chats.KEY_PICTURE, chatEntry.pictureHash);
		values.put(Chats.FOREIGN_GROUP, chatEntry.groupID);
		values.put(Chats.FOREIGN_USER, chatEntry.userID);
		values.put(Chats.FOREIGN_ROOM, chatroomID);
	}

	*/
/**
	 * Save ChatEntries to DB
	 *
	 * @param entries All entries that have a higher chatID than this.lastID will be saved to DB
	 *//*

	private synchronized void saveChats(List<ChatEntry> entries) {
		SQLiteDatabase db = server.db;
		//KEY_ID, KEY_TIME, FOREIGN_GROUP, FOREIGN_USER, KEY_MESSAGE, KEY_PICTURE, FOREIGN_ROOM
		SQLiteStatement s = db.compileStatement("INSERT INTO " + Chats.TABLE +
				" (" + DatabaseHelper.strStr(Chats.COLS) + ") VALUES (?, ?, ?, ?, ?, ?, " + chatroomID + ")");

		int chatId;

		try {
			ChatEntry c;
			for (Iterator<ChatEntry> i = entries.iterator(); i.hasNext(); ) {
				c = i.next();

				if (c.chatID <= lastId) {
					i.remove();
					continue;
				}

				try {
					//				Log.d(THIS, "Inserted "+c+" in Messages");

					s.bindLong(1, c.chatID);
					s.bindLong(2, c.timestamp);
					s.bindLong(3, c.groupID);
					s.bindLong(4, c.userID);
					s.bindString(5, c.message);

					if (c.pictureHash != null)
						s.bindLong(6, c.pictureHash);
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

		} catch (Exception e) {
			Log.e(THIS, "All Inserts failed", e);
		} finally {
			s.close();
		}

		Log.i(THIS, "Received " + entries.size() + " new Chats in Chatroom " + chatroomID + " since " + this.lastRefresh + "");

//		notifyChatsAdded(entries);
		notifyChatsChanged();
	}

	private void saveChat(ChatEntry chat) {
		ContentValues insert = new ContentValues();
		insert.put(Chats.KEY_ID, chat.chatID);

		fillContentValues(insert, chat);

		server.db.insert(Chats.TABLE, null, insert);

		setLast(chat.timestamp, chat.chatID);
	}

	private synchronized void setLast(long refresh, int id) {
		if (refresh > 0) {
			if (refresh > lastRefresh)
				lastRefresh = refresh;
			else
				Log.e(THIS, "Outdated lastID: old:" + lastId + ", new: " + id);
		}

		if (id > 0) {
			if (id > lastId)
				lastId = id;
			else
				Log.e(THIS, "Outdated lastID: old:" + lastId + ", new: " + id);
		}
	}

	private synchronized void setState(final ChatroomState newState) {
		state = newState;

		Log.i(THIS, "Status: " + newState);

		server.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IChatroomListener l : listeners) {
					l.onStateChanged(newState);
				}
			}
		});
	}

	@Override
	public synchronized ChatroomState getChatStatus() {
		return state;
	}

	@Override
	public void addListener(IChatroomListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(IChatroomListener l) {
		listeners.remove(l);
	}

//	*/
/**
//	 * Request a callback with all available Chats
//	 * Callback pattern in anticipation of asynchronous DB access
//	 * @param callback the single Listener that wants to completely refresh or initialize its content
//	 *//*

//	@Override
//	public void provideChats(IChatroomListener callback) {
//		Log.i(THIS, "Chats requested");
//		callback.chatsProvided(getAllChats());
//	}

//	private void notifyChatsEdited(final List<ChatEntry> entries) {
//		server.uiHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				for(IChatroomListener l: listeners) {
//					l.chatsEdited(entries);
//				}
//			}
//		});
//	}

//	private void notifyChatsAdded(final List<ChatEntry> entries) {
//		server.uiHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				for(IChatroomListener l: listeners) {
//					l.chatsAdded(entries);
//				}
//			}
//		});
//	}

	*/
/**
	 * Notify all listeners on UI Thread if any
	 * @return false if no listeners to notify
	 *//*

	private boolean notifyChatsChanged() {
		if (listeners.size() > 0) {

			server.uiHandler.post(new Runnable() {
				@Override
				public void run() {
					for (IChatroomListener l : listeners) {
						l.onChatsChanged();
					}
				}
			});
			return true;
		} else
			return false;
	}


	private void notifyChatPostState(final PostState state, final int postID, final ChatEntry chatEntry) {
		server.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IChatroomListener l : listeners) {
					l.onPostStateChange(postID, state, chatEntry);
				}
			}
		});
	}

	void onDbChange() {
		server.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IChatroomListener l : listeners) {
					l.onChatsChanged();
				}
			}
		});
	}

	*/
/**
	 * Retrieve all Chats from DB
	 * if a groupName or userName cannot be found in the DB, an update of the Table is initiated and the Chats will be edited as soon as the information is available
	 * @return preliminary chatEntries, will be edited if incomplete
	 *//*

//	private List<ChatEntry> getAllChats() {
//		Cursor c = server.db.query(Chats.TABLE +" AS c LEFT JOIN "+ Groups.TABLE +" AS g USING("+Chats.FOREIGN_GROUP+") LEFT JOIN "+ Users.TABLE +" AS u USING("+Chats.FOREIGN_USER+")",
//				new String[]{Chats.KEY_ID, Chats.KEY_MESSAGE, Chats.KEY_TIME, "c."+Chats.FOREIGN_GROUP, Groups.KEY_NAME, Chats.FOREIGN_USER, Users.KEY_NAME, Chats.KEY_PICTURE}, Chatrooms.KEY_ID+"="+id,	null, null, null, Chats.KEY_TIME);
//
//		final ArrayList<ChatEntry> out = new ArrayList<>(),
//					missingUser = new ArrayList<>(),
//					missingGroup = new ArrayList<>();
//
//		while (c.moveToNext()) {
//			String groupName = c.getString(4), userName = c.getString(6);
//			ChatEntry chatEntry = new ChatEntry(c.getInt(0), c.getString(1), c.getLong(2), c.getInt(3), groupName, c.getInt(5), userName, c.getInt(7));
//			out.add(chatEntry);
//			if (groupName == null) {
//				missingGroup.add(chatEntry);
//			}
//
//			if (userName == null) {
//				missingUser.add(chatEntry);
//			}
//		}
//		c.close();
//
//		if (missingGroup.size() > 0) {
//			server.getAvailableGroups(new IModel.IListAvailableCallback<Group>() {
//				@Override
//				public void dataAvailable(List<Group> groups) {
//					HashMap<Integer, Group> groupMap = new HashMap<>();
//					for (Group g: groups) {
//						groupMap.put(g.groupID, g);
//					}
//
//					for (ChatEntry chat: missingGroup) {
//						chat.setGroupName(groupMap.get(chat.groupID).name);
//					}
//					notifyChatsEdited(missingGroup);
//				}
//			});
//		}
//		if (missingUser.size() > 0) {
//			server.getAllUsers(new IModel.IMapAvailableCallback<Integer, GroupUser>() {
//				@Override
//				public void dataAvailable(Map<Integer, GroupUser> users) {
//					for (ChatEntry chat: missingUser) {
//						chat.setUserName(users.get(chat.userID).name);
//					}
//					notifyChatsEdited(missingUser);
//				}
//			});
//		}
//		return out;
//	}

	@Override
	public Cursor getChatCursor() {
		Cursor c = server.db.query(Chats.TABLE + " AS c LEFT JOIN " + Groups.TABLE + " AS g USING(" + Chats.FOREIGN_GROUP + ") LEFT JOIN " + Users.TABLE + " AS u USING(" + Chats.FOREIGN_USER + ")",
				new String[]{Chats.KEY_ID + " AS _id", Chats.KEY_MESSAGE, Chats.KEY_TIME, "c." + Chats.FOREIGN_GROUP, Groups.KEY_NAME, Chats.FOREIGN_USER, Users.KEY_NAME, Chats.KEY_PICTURE}, Chatrooms.KEY_ID + "=" + chatroomID, null, null, null, Chats.KEY_ID);
		Log.i(THIS, "new Cursor: " + c.getCount() + " rows");
		return c;
	}

	private static class PictureGallery extends de.stadtrallye.rallyesoft.server.PictureGallery {

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
		Cursor c = server.db.query(Chats.TABLE, new String[]{Chats.KEY_PICTURE}, Chats.KEY_PICTURE + " <> 0 AND " + Chats.FOREIGN_ROOM + " = ?", new String[]{Integer.toString(chatroomID)}, Chats.KEY_PICTURE, null, Chats.KEY_TIME);
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

		return new PictureGallery(initialPos, pictures, server.getPictureIdResolver());
	}

	boolean needPost = true;
	@Override
	public void setLastReadId(int lastRead) {
		if (lastReadId < lastRead) {
			lastReadId = lastRead;
			if (needPost) {
				needPost = false;
				server.uiHandler.post(new Runnable() {
					@Override
					public void run() {
						needPost = true;
						ChatNotificationManager.getInstance(server.context).updateNotification();
					}
				});
			}
		}
	}

	@Override
	public int getLastReadId() {
		return lastReadId;
	}

	@Override
	public List<ChatEntry> getUnreadEntries() {
		Cursor cursor = getChatCursor();

		CursorConverters.ChatCursorIds c = CursorConverters.ChatCursorIds.read(cursor);

		CursorConverters.moveCursorToId(cursor,c.id,lastReadId);

		List<ChatEntry> res = new ArrayList<>();

		cursor.moveToNext();
		while (!cursor.isAfterLast()) {
			res.add(CursorConverters.getChatEntry(cursor,c));
			cursor.moveToNext();
		}
		return res;


	}

	@Override
	public de.rallye.server.structures.SimpleChatEntry postChat(String msg, Integer pictureHash) {
		if (!server.isConnected()) {
			err.notLoggedIn();
			return -1;
		}
		try {
			int taskId = nextTaskId++;
			server.exec.execute(new JSONObjectRequestExecutor<ChatEntry, AdvTaskId>(server.factory.chatPostRequest(chatroomID, msg, pictureHash), new JsonConverters.ChatConverter(), this, new AdvTaskId(Tasks.CHAT_POST, taskId)));
			return taskId;
		} catch (HttpRequestException e) {
			err.requestException(e);
			return -1;
		}
	}

	@Override
	public de.rallye.server.structures.SimpleChatWithPictureHash postChatWithHash(String msg, String pictureHash) {
		if (!server.isConnected()) {
			err.notLoggedIn();
			return -1;
		}
		try {
			int taskId = nextTaskId++;
			server.exec.execute(new JSONObjectRequestExecutor<ChatEntry, AdvTaskId>(server.factory.chatPostWithHashRequest(chatroomID, msg, pictureHash), new JsonConverters.ChatConverter(), this, new AdvTaskId(Tasks.CHAT_POST, taskId)));
			return taskId;
		} catch (HttpRequestException e) {
			err.requestException(e);
			return -1;
		}
	}

	private void chatPostResult(RequestExecutor<ChatEntry, ?> r, int postID) {
		if (r.isSuccessful()) {
			ChatEntry chat = r.getResult();

			saveChat(chat);

//			lookupNames(chat);
			notifyChatPostState(PostState.Success, postID, chat);
			notifyChatsChanged();
		} else {
			notifyChatPostState(PostState.Failure, postID, null);
			server.commError(null);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void executorResult(RequestExecutor<?, AdvTaskId> r, AdvTaskId callbackId) {
		switch (callbackId.task) {
			case CHAT_POST:
				chatPostResult((RequestExecutor<ChatEntry, ?>) r, callbackId.id);
				break;
			case CHAT_REFRESH:
				chatRefreshResult((RequestExecutor<List<ChatEntry>, ?>) r);
				break;
			default:
				Log.e(THIS, "Unknown Executor Callback");
				break;
		}
	}
}
*/
