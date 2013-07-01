package de.stadtrallye.rallyesoft.model;

import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.GroupUser;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.Chatroom.AdvTaskId;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Chatrooms;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Chats;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Groups;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Users;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.JSONObjectRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class Chatroom implements IChatroom, RequestExecutor.Callback<AdvTaskId> {
	
	// statics
	final private static String CLASS = Chatroom.class.getSimpleName();
	final private String THIS;
	final private static ErrorHandling err = new ErrorHandling(CLASS);

	private enum Tasks { CHAT_REFRESH, CHAT_POST }

	public static class AdvTaskId {
		final public Tasks task;
		final public int id;

		public AdvTaskId(Tasks task, int id) {
			this.task = task;
			this.id = id;
		}
	}

    // members
	final private Model model;
	final private int id;
	final private String name;
	private long lastRefresh = 0;//Conservative: timestamp of latest ChatEntry
	private int lastId = 0;

	private int nextTaskId = 0;
	
	private ChatroomState status;
	
	private ArrayList<IChatroomListener> listeners = new ArrayList<>();

    /**
     *
     * @return all available Chatrooms
     */
	static List<Chatroom> getChatrooms(Model model) {
		List<Chatroom> out = new ArrayList<>();

		Cursor c = model.db.query(Chatrooms.TABLE, Chatrooms.COLS, null, null, null, null, null);
		
		while (c.moveToNext()) {
			Chatroom room = new Chatroom(c.getInt(0), c.getString(1), model);
			room.refresh();
			out.add(room);
		}

		Log.i(CLASS, "Restored "+ out.size() +" Chatrooms");
		return out;
	}

	static void saveChatrooms(Model model, List<Chatroom> chatrooms) {
		SQLiteDatabase db = model.db;

		db.delete(Chatrooms.TABLE, null, null);

		for (Chatroom c: chatrooms) {
			ContentValues insert = new ContentValues();
			insert.put(Chatrooms.KEY_ID, c.id);
			insert.put(Chatrooms.KEY_NAME, c.name);
			insert.put(Chatrooms.KEY_LAST_REFRESH, c.lastRefresh);
			db.insert(Chatrooms.TABLE, null, insert);
		}
	}

	private Chatroom(int id, String name, Model model) {
		this(id, name, model, 0);
	}
	
	private Chatroom(int id, String name, Model model, long lastRefresh) {
		this.id = id;
		this.name = name;
		this.model = model;
		this.lastRefresh = lastRefresh;

		THIS = CLASS +" "+ id;

		Cursor c = model.db.query(Chats.TABLE, Chats.COLS, Chats.FOREIGN_ROOM+"="+id, null, null, null, Chats.KEY_ID +" DESC", "1");

		if (c.moveToNext()) {
			this.lastId =  c.getInt(0);
		}
		c.close();

		Log.i(THIS, "Chatroom restored: "+ id +", mostRecent:"+ lastId +", lastRefresh:"+ lastRefresh);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Chatroom chatroom = (Chatroom) o;

		return id == chatroom.id;
	}

	public static class ChatroomConverter extends JSONConverter<Chatroom> {
		
		private Model model;

		public ChatroomConverter(Model model) {
			this.model = model;
		}
		
		@Override
		public Chatroom doConvert(JSONObject o) throws JSONException {
			int i = o.getInt(de.rallye.model.structures.Chatroom.CHATROOM_ID);
			String name = o.getString(de.rallye.model.structures.Chatroom.NAME);
			
			return new Chatroom(i, name, model);
		}
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
		if (status != ChatroomState.Ready) {
			err.concurrentRefresh();
		}
		
		try {
			setChatStatus(ChatroomState.Refreshing);

			model.exec.execute(new JSONArrayRequestExecutor<>(model.factory.chatRefreshRequest(id, lastRefresh), new ChatEntry.ChatConverter(), this, new AdvTaskId(Tasks.CHAT_REFRESH, 0)));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void chatRefreshResult(RequestExecutor<List<ChatEntry>, ?> r) {
		if (r.isSuccessful()){
			List<ChatEntry> res = r.getResult();

			if (res.size() > 0) {
				saveChats(res);
			} else
				Log.i(THIS, "No new Entries");
			setChatStatus(ChatroomState.Ready);
		} else {
			err.asyncTaskResponseError(r.getException());
			model.commError(r.getException());
		}
	}

	@Override
	public void addChat(ChatEntry chatEntry) {
		saveChat(chatEntry);

		lookupNames(chatEntry);

		List<ChatEntry> upd = new ArrayList<>();
		upd.add(chatEntry);

		Log.i(THIS, "Pushed: "+ chatEntry);

		notifyChatsAdded(upd);
	}

	@Override
	public void editChat(ChatEntry chatEntry) {
		ContentValues update = new ContentValues();

		fillContentValues(update, chatEntry);

		model.db.update(Chats.TABLE, update, Chats.KEY_ID+"="+chatEntry.chatID, null);

		//TODO: setLast()
		setLast(chatEntry.timestamp, 0);

		lookupNames(chatEntry);

		List<ChatEntry> upd = new ArrayList<>();
		upd.add(chatEntry);

		notifyChatsEdited(upd);
	}

	private void lookupNames(final ChatEntry chatEntry) {
		Cursor c = model.db.query(Users.TABLE, Users.COLS, Users.KEY_ID+"="+chatEntry.userID, null, null, null, null);
		if (c.moveToFirst()) {
			chatEntry.setUserName(c.getString(1));
		} else {
			model.getAllUsers(new IModel.IMapAvailableCallback<Integer, GroupUser>() {
				@Override
				public void dataAvailable(Map<Integer, GroupUser> users) {
					chatEntry.setUserName(users.get(chatEntry.userID).name);

					List<ChatEntry> list = new ArrayList<>();
					list.add(chatEntry);
					notifyChatsEdited(list);
				}
			});
		}
		c.close();

		c = model.db.query(Groups.TABLE, Groups.COLS, Groups.KEY_ID+"="+chatEntry.groupID, null, null, null, null);
		if (c.moveToFirst()) {
			chatEntry.setGroupName(c.getString(1));
		} else {
			model.getAvailableGroups(new IModel.IListAvailableCallback<Group>() {
				@Override
				public void dataAvailable(List<Group> groups) {
					HashMap<Integer, Group> groupMap = new HashMap<>();
					for (Group g: groups) {
						groupMap.put(g.groupID, g);
					}

					chatEntry.setGroupName(groupMap.get(chatEntry.groupID).name);

					List<ChatEntry> list = new ArrayList<>();
					list.add(chatEntry);
					notifyChatsEdited(list);
				}
			});
		}
	}

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
	 * @param entries All entries that have a higher chatID than this.lastID will be saved to DB
	 */
	private synchronized void saveChats(List<ChatEntry> entries) {
		SQLiteDatabase db = model.db;
		//KEY_ID, KEY_TIME, FOREIGN_GROUP, FOREIGN_USER, KEY_MESSAGE, KEY_PICTURE, FOREIGN_ROOM
		SQLiteStatement s = db.compileStatement("INSERT INTO "+ Chats.TABLE +
				" ("+ DatabaseHelper.strStr(Chats.COLS) +") VALUES (?, ?, ?, ?, ?, ?, "+ id +")");
		
		int chatId;

		try {
			ChatEntry c;
			for (Iterator<ChatEntry> i = entries.iterator(); i.hasNext();) {
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
					s.bindLong(6, (c.pictureID != null)? c.pictureID : 0);
					chatId = (int) s.executeInsert();
					
	//				Log.d(THIS, "Inserted "+c+" in Chats");

					if (chatId != c.chatID)
						throw new SQLDataException();
					
				} catch (Exception e) {
					Log.e(THIS, "Single Insert failed", e);
				}
			}
			
			if (entries.size() > 0) {
				ChatEntry last = entries.get(entries.size()-1);

				setLast(last.timestamp, last.chatID);
			}

		} catch (Exception e) {
			Log.e(THIS, "All Inserts failed", e);
		} finally {
			s.close();
		}
		
		Log.i(THIS, "Received "+ entries.size() +" new Chats in Chatroom "+ this.id +" since "+ this.lastRefresh +"");
		
		notifyChatsAdded(entries);
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
				Log.e(THIS, "Outdated lastID: old:"+ lastId +", new: "+ id);
		}

		if (id > 0) {
			if (id > lastId)
				lastId = id;
			else
				Log.e(THIS, "Outdated lastID: old:"+ lastId +", new: "+ id);
		}
	}

	private void setChatStatus(final ChatroomState newState) {
		status = newState;

		Log.i(THIS, "Status: "+ newState);

		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IChatroomListener l: listeners) {
					l.onChatStatusChanged(newState);
				}
			}
		});
	}

	@Override
	public ChatroomState getChatStatus() {
		return status;
	}

	@Override
	public void addListener(IChatroomListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(IChatroomListener l) {
		listeners.remove(l);
	}

	/**
	 * Request a callback with all available Chats
	 * Callback pattern in anticipation of asynchronous DB access
	 * @param callback the single Listener that wants to completely refresh or initialize its content
	 */
	@Override//TODO: switch to CursorAdapter and Cursor as return type
	public void provideChats(IChatroomListener callback) {
		Log.i(THIS, "Chats requested");
		callback.chatsProvided(getAllChats());
	}
	
	private void notifyChatsEdited(final List<ChatEntry> entries) {
		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for(IChatroomListener l: listeners) {
					l.chatsEdited(entries);
				}
			}
		});
	}
	
	private void notifyChatsAdded(final List<ChatEntry> entries) {
		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for(IChatroomListener l: listeners) {
					l.chatsAdded(entries);
				}
			}
		});
	}

	private void notifyChatPostState(final PostState state, final int postID, final ChatEntry chatEntry) {
		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for(IChatroomListener l: listeners) {
					l.onPostStateChange(postID, state, chatEntry);
				}
			}
		});
	}

	/**
	 * Retrieve all Chats from DB
	 * if a groupName or userName cannot be found in the DB, an update of the Table is initiated and the Chats will be edited as soon as the information is available
	 * @return preliminary chatEntries, will be edited if incomplete
	 */
	private List<ChatEntry> getAllChats() {
		Cursor c = model.db.query(Chats.TABLE +" AS c LEFT JOIN "+ Groups.TABLE +" AS g USING("+Chats.FOREIGN_GROUP+") LEFT JOIN "+ Users.TABLE +" AS u USING("+Chats.FOREIGN_USER+")",
				new String[]{Chats.KEY_ID, Chats.KEY_MESSAGE, Chats.KEY_TIME, "c."+Chats.FOREIGN_GROUP, Groups.KEY_NAME, Chats.FOREIGN_USER, Users.KEY_NAME, Chats.KEY_PICTURE}, Chatrooms.KEY_ID+"="+id,	null, null, null, Chats.KEY_TIME);
		
		final ArrayList<ChatEntry> out = new ArrayList<>(),
					missingUser = new ArrayList<>(),
					missingGroup = new ArrayList<>();
		
		while (c.moveToNext()) {
			String groupName = c.getString(4), userName = c.getString(6);
			ChatEntry chatEntry = new ChatEntry(c.getInt(0), c.getString(1), c.getLong(2), c.getInt(3), groupName, c.getInt(5), userName, c.getInt(7));
			out.add(chatEntry);
			if (groupName == null) {
				missingGroup.add(chatEntry);
			}

			if (userName == null) {
				missingUser.add(chatEntry);
			}
		}
		c.close();

		if (missingGroup.size() > 0) {
			model.getAvailableGroups(new IModel.IListAvailableCallback<Group>() {
				@Override
				public void dataAvailable(List<Group> groups) {
					HashMap<Integer, Group> groupMap = new HashMap<>();
					for (Group g: groups) {
						groupMap.put(g.groupID, g);
					}

					for (ChatEntry chat: missingGroup) {
						chat.setGroupName(groupMap.get(chat.groupID).name);
					}
					notifyChatsEdited(missingGroup);
				}
			});
		}
		if (missingUser.size() > 0) {
			model.getAllUsers(new IModel.IMapAvailableCallback<Integer, GroupUser>() {
				@Override
				public void dataAvailable(Map<Integer, GroupUser> users) {
					for (ChatEntry chat: missingUser) {
						chat.setUserName(users.get(chat.userID).name);
					}
					notifyChatsEdited(missingUser);
				}
			});
		}
		return out;
	}

	public static class ChatCursor {
		public static final int chatID = 0;
		public static final int message = 1;
		public static final int timestamp = 2;
		public static final int groupID = 3;
		public static final int groupName = 4;
		public static final int userID = 5;
		public static final int userName = 6;
		public static final int pictureID = 7;
	}

	@Override
	public Cursor getChatCursor() {
		Cursor c = model.db.query(Chats.TABLE +" AS c LEFT JOIN "+ Groups.TABLE +" AS g USING("+Chats.FOREIGN_GROUP+") LEFT JOIN "+ Users.TABLE +" AS u USING("+Chats.FOREIGN_USER+")",
				new String[]{Chats.KEY_ID +" AS _id", Chats.KEY_MESSAGE, Chats.KEY_TIME, "c."+Chats.FOREIGN_GROUP, Groups.KEY_NAME, Chats.FOREIGN_USER, Users.KEY_NAME, Chats.KEY_PICTURE}, Chatrooms.KEY_ID+"="+id,	null, null, null, Chats.KEY_TIME);
		Log.i(THIS, "new Cursor: "+ c.getCount() +" rows");
		return c;
	}
	
	private class PictureGallery extends AbstractPictureGallery {
		
		public PictureGallery(int initialPictureID) {
			this.pictures = new ArrayList<>();
			
			Cursor c = model.db.query(Chats.TABLE, new String[]{Chats.KEY_PICTURE}, Chats.KEY_PICTURE+" <> 0 AND "+Chats.FOREIGN_ROOM+" = ?", new String[]{Integer.toString(Chatroom.this.id)}, Chats.KEY_PICTURE, null, Chats.KEY_TIME);
			
			int picId, i = 0;
			while (c.moveToNext()) {
				picId = c.getInt(0);
				this.pictures.add(picId);
				if (picId == initialPictureID) {
					initialPos = i;
				}
				i++;
			}
			c.close();
		}
		
		@Override
		public String getPictureUrl(int pos) {
			return model.getUrlFromImageId(this.pictures.get(pos), size);
		}
		
	}

	@Override
	public IPictureGallery getPictureGallery(int initialPictureId) {
		return new PictureGallery(initialPictureId);
	}
	
	@Override//TODO
	public void saveCurrentState(int lastRead) {
		
	}

	@Override//TODO
	public int getLastState() {
		return 0;
	}

	@Override
	public int postChat(String msg, Integer pictureID) {
		if (!model.isConnected()) {
            err.notLoggedIn();
            return -1;
        }
        try {
			int taskId = nextTaskId++;
        	model.exec.execute(new JSONObjectRequestExecutor<>(model.factory.chatPostRequest(id, msg, pictureID), new ChatEntry.ChatConverter(), this, new AdvTaskId(Tasks.CHAT_POST, taskId)));
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

			lookupNames(chat);
			notifyChatPostState(PostState.Success, postID, chat);
		} else {
			notifyChatPostState(PostState.Failure, postID, null);
			model.commError(null);//TODO: better exception handling for communication breakdown
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
