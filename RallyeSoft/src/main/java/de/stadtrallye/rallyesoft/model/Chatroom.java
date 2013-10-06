package de.stadtrallye.rallyesoft.model;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.stadtrallye.rallyesoft.MainActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.Chatroom.AdvTaskId;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;
import de.stadtrallye.rallyesoft.model.converters.JsonConverters;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Chatrooms;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Chats;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Groups;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Users;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.JSONObjectRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.uimodel.ChatCursorAdapter;

import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.EDIT_CHATS;

public class Chatroom implements IChatroom, RequestExecutor.Callback<AdvTaskId> {

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
	final private Model model;

	// Identifier
	final private int id;
	final private String name;

	// State
	private ChatroomState state;
	private long lastRefresh = 0;//Conservative: timestamp of latest ChatEntry
	private int lastId = 0;
	private int lastReadId;

	// External Ids for asynchronous tasks (Posting a chat etc.)
	private int nextTaskId = 0;

	//Listeners
	private final ArrayList<IChatroomListener> listeners = new ArrayList<IChatroomListener>();

	/**
	 * @return all available Chatrooms
	 */
	static List<Chatroom> getChatrooms(Model model) {
		List<Chatroom> out = new ArrayList<Chatroom>();

		Cursor c = model.db.query(Chatrooms.TABLE, Chatrooms.COLS, null, null, null, null, null);

		while (c.moveToNext()) {
			Chatroom room = new Chatroom(c.getInt(0), c.getString(1), model, c.getLong(2), c.getInt(3));

			if ((model.deprecatedTables & EDIT_CHATS) > 0) {
				room.resync();
				model.deprecatedTables &= ~EDIT_CHATS;
			}

			room.refresh();
			out.add(room);
		}

		Log.i(CLASS, "Restored " + out.size() + " Chatrooms");
		return out;
	}

	static void saveChatrooms(Model model, List<Chatroom> chatrooms) {
		if (chatrooms == null)
			return;

		SQLiteDatabase db = model.db;

		db.delete(Chatrooms.TABLE, null, null);

		for (Chatroom c : chatrooms) {
			ContentValues insert = new ContentValues();
			insert.put(Chatrooms.KEY_ID, c.id);
			insert.put(Chatrooms.KEY_NAME, c.name);
			insert.put(Chatrooms.KEY_LAST_REFRESH, c.lastRefresh);
			insert.put(Chatrooms.KEY_LAST_READ, c.lastReadId);
			db.insert(Chatrooms.TABLE, null, insert);
		}
	}

	public Chatroom(int id, String name, Model model) {
		this(id, name, model, 0, 0);
	}

	private Chatroom(int id, String name, Model model, long lastRefresh, int lastReadId) {
		this.id = id;
		this.name = name;
		this.model = model;
		this.lastRefresh = lastRefresh;
		this.lastReadId = lastReadId;

		THIS = CLASS + " " + id;

		Cursor c = model.db.query(Chats.TABLE, Chats.COLS, Chats.FOREIGN_ROOM + "=" + id, null, null, null, Chats.KEY_ID + " DESC", "1");

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
		if (o == null || getClass() != o.getClass()) return false;

		Chatroom chatroom = (Chatroom) o;

		return id == chatroom.id;
	}

	// Implementation
	@Override
	public int getID() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public synchronized void refresh() {
		if (!model.isConnected()) {
			err.notLoggedIn();
			return;
		}
		if (state != ChatroomState.Ready) {
			err.concurrentRefresh();
			return;
		}

		try {
			setState(ChatroomState.Refreshing);

			model.exec.execute(new JSONArrayRequestExecutor<ChatEntry, AdvTaskId>(model.factory.chatRefreshRequest(id, lastRefresh), new JsonConverters.ChatConverter(), this, new AdvTaskId(Tasks.CHAT_REFRESH, 0)));
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
			model.commError(r.getException());
		}
	}

	@Override
	public synchronized void resync() {
		lastId = 0;
		lastRefresh = 0;

		model.db.delete(Chats.TABLE, null, null);
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
			ChatNotificationManager.getInstance(model.context).updateNotification();
	}

	@Override
	public void editChat(ChatEntry chatEntry) {
		ContentValues update = new ContentValues();

		fillContentValues(update, chatEntry);

		model.db.update(Chats.TABLE, update, Chats.KEY_ID + "=" + chatEntry.chatID, null);

		setLast(chatEntry.timestamp, 0);

//		lookupNames(chatEntry);
//
//		List<ChatEntry> upd = new ArrayList<>();
//		upd.add(chatEntry);

//		notifyChatsEdited(upd);
		notifyChatsChanged();
	}

//	private void lookupNames(final ChatEntry chatEntry) {
//		Cursor c = model.db.query(Users.TABLE, Users.COLS, Users.KEY_ID+"="+chatEntry.userID, null, null, null, null);
//		if (c.moveToFirst()) {
//			chatEntry.setUserName(c.getString(1));
//		} else {
//			model.getAllUsers(new IModel.IMapAvailableCallback<Integer, GroupUser>() {
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
//		c = model.db.query(Groups.TABLE, Groups.COLS, Groups.KEY_ID+"="+chatEntry.groupID, null, null, null, null);
//		if (c.moveToFirst()) {
//			chatEntry.setGroupName(c.getString(1));
//		} else {
//			model.getAvailableGroups(new IModel.IListAvailableCallback<Group>() {
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

	/**
	 * Helper to remove redundancy of adding all Attributes to a ContentValues Map (except chatID)
	 */
	private void fillContentValues(ContentValues values, ChatEntry chatEntry) {
		values.put(Chats.KEY_MESSAGE, chatEntry.message);
		values.put(Chats.KEY_TIME, chatEntry.timestamp);
		values.put(Chats.KEY_PICTURE, chatEntry.pictureID);
		values.put(Chats.FOREIGN_GROUP, chatEntry.groupID);
		values.put(Chats.FOREIGN_USER, chatEntry.userID);
		values.put(Chats.FOREIGN_ROOM, id);
	}

	/**
	 * Save ChatEntries to DB
	 *
	 * @param entries All entries that have a higher chatID than this.lastID will be saved to DB
	 */
	private synchronized void saveChats(List<ChatEntry> entries) {
		SQLiteDatabase db = model.db;
		//KEY_ID, KEY_TIME, FOREIGN_GROUP, FOREIGN_USER, KEY_MESSAGE, KEY_PICTURE, FOREIGN_ROOM
		SQLiteStatement s = db.compileStatement("INSERT INTO " + Chats.TABLE +
				" (" + DatabaseHelper.strStr(Chats.COLS) + ") VALUES (?, ?, ?, ?, ?, ?, " + id + ")");

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

		} catch (Exception e) {
			Log.e(THIS, "All Inserts failed", e);
		} finally {
			s.close();
		}

		Log.i(THIS, "Received " + entries.size() + " new Chats in Chatroom " + this.id + " since " + this.lastRefresh + "");

//		notifyChatsAdded(entries);
		notifyChatsChanged();
	}

	private void saveChat(ChatEntry chat) {
		ContentValues insert = new ContentValues();
		insert.put(Chats.KEY_ID, chat.chatID);

		fillContentValues(insert, chat);

		model.db.insert(Chats.TABLE, null, insert);

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

		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IChatroomListener l : listeners) {
					l.onChatroomStateChanged(newState);
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

//	/**
//	 * Request a callback with all available Chats
//	 * Callback pattern in anticipation of asynchronous DB access
//	 * @param callback the single Listener that wants to completely refresh or initialize its content
//	 */
//	@Override
//	public void provideChats(IChatroomListener callback) {
//		Log.i(THIS, "Chats requested");
//		callback.chatsProvided(getAllChats());
//	}

//	private void notifyChatsEdited(final List<ChatEntry> entries) {
//		model.uiHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				for(IChatroomListener l: listeners) {
//					l.chatsEdited(entries);
//				}
//			}
//		});
//	}

//	private void notifyChatsAdded(final List<ChatEntry> entries) {
//		model.uiHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				for(IChatroomListener l: listeners) {
//					l.chatsAdded(entries);
//				}
//			}
//		});
//	}

	/**
	 * Notify all listeners on UI Thread if any
	 * @return false if no listeners to notify
	 */
	private boolean notifyChatsChanged() {
		if (listeners.size() > 0) {

			model.uiHandler.post(new Runnable() {
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
		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IChatroomListener l : listeners) {
					l.onPostStateChange(postID, state, chatEntry);
				}
			}
		});
	}

	void onDbChange() {
		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IChatroomListener l : listeners) {
					l.onChatsChanged();
				}
			}
		});
	}

	/**
	 * Retrieve all Chats from DB
	 * if a groupName or userName cannot be found in the DB, an update of the Table is initiated and the Chats will be edited as soon as the information is available
	 * @return preliminary chatEntries, will be edited if incomplete
	 */
//	private List<ChatEntry> getAllChats() {
//		Cursor c = model.db.query(Chats.TABLE +" AS c LEFT JOIN "+ Groups.TABLE +" AS g USING("+Chats.FOREIGN_GROUP+") LEFT JOIN "+ Users.TABLE +" AS u USING("+Chats.FOREIGN_USER+")",
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
//			model.getAvailableGroups(new IModel.IListAvailableCallback<Group>() {
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
//			model.getAllUsers(new IModel.IMapAvailableCallback<Integer, GroupUser>() {
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
		Cursor c = model.db.query(Chats.TABLE + " AS c LEFT JOIN " + Groups.TABLE + " AS g USING(" + Chats.FOREIGN_GROUP + ") LEFT JOIN " + Users.TABLE + " AS u USING(" + Chats.FOREIGN_USER + ")",
				new String[]{Chats.KEY_ID + " AS _id", Chats.KEY_MESSAGE, Chats.KEY_TIME, "c." + Chats.FOREIGN_GROUP, Groups.KEY_NAME, Chats.FOREIGN_USER, Users.KEY_NAME, Chats.KEY_PICTURE}, Chatrooms.KEY_ID + "=" + id, null, null, null, Chats.KEY_ID);
		Log.i(THIS, "new Cursor: " + c.getCount() + " rows");
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
		Cursor c = model.db.query(Chats.TABLE, new String[]{Chats.KEY_PICTURE}, Chats.KEY_PICTURE + " <> 0 AND " + Chats.FOREIGN_ROOM + " = ?", new String[]{Integer.toString(Chatroom.this.id)}, Chats.KEY_PICTURE, null, Chats.KEY_TIME);
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

		return new PictureGallery(initialPos, pictures, model.getPictureIdResolver());
	}

	boolean needPost = true;
	@Override
	public void setLastReadId(int lastRead) {
		if (lastReadId < lastRead) {
			lastReadId = lastRead;
			if (needPost) {
				needPost = false;
				model.uiHandler.post(new Runnable() {
					@Override
					public void run() {
						needPost = true;
						ChatNotificationManager.getInstance(model.context).updateNotification();
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

		List<ChatEntry> res = new ArrayList<ChatEntry>();

		cursor.moveToNext();
		while (!cursor.isAfterLast()) {
			res.add(CursorConverters.getChatEntry(cursor,c));
			cursor.moveToNext();
		}
		return res;


	}

	@Override
	public int postChat(String msg, Integer pictureID) {
		if (!model.isConnected()) {
			err.notLoggedIn();
			return -1;
		}
		try {
			int taskId = nextTaskId++;
			model.exec.execute(new JSONObjectRequestExecutor<ChatEntry, AdvTaskId>(model.factory.chatPostRequest(id, msg, pictureID), new JsonConverters.ChatConverter(), this, new AdvTaskId(Tasks.CHAT_POST, taskId)));
			return taskId;
		} catch (HttpRequestException e) {
			err.requestException(e);
			return -1;
		}
	}

	@Override
	public int postChatWithHash(String msg, String pictureHash) {
		if (!model.isConnected()) {
			err.notLoggedIn();
			return -1;
		}
		try {
			int taskId = nextTaskId++;
			model.exec.execute(new JSONObjectRequestExecutor<ChatEntry, AdvTaskId>(model.factory.chatPostWithHashRequest(id, msg, pictureHash), new JsonConverters.ChatConverter(), this, new AdvTaskId(Tasks.CHAT_POST, taskId)));
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
			model.commError(null);
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
