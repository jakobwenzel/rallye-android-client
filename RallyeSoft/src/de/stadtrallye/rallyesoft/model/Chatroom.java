package de.stadtrallye.rallyesoft.model;

import java.sql.SQLDataException;
import java.util.*;
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
import de.stadtrallye.rallyesoft.model.Chatroom.Tasks;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Chatrooms;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Chats;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Groups;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Users;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class Chatroom implements IChatroom, RequestExecutor.Callback<Tasks> {
	
	// statics
	final private static String CLASS = Chatroom.class.getSimpleName();
	final private String THIS;
	final private static ErrorHandling err = new ErrorHandling(CLASS);

	enum Tasks { CHAT_REFRESH, CHAT_POST }

    // members
	final private Model model;
	final private int id;
	final private String name;
	private long lastUpdate = 0;
	private int lastId = 0;
	
	private ChatStatus status;
	
	private ArrayList<IChatListener> listeners = new ArrayList<>();

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
			db.insert(Chatrooms.TABLE, null, insert);
		}
	}
	
	private Chatroom(int id, String name, Model model) {
		this.id = id;
		this.name = name;
		this.model = model;

		THIS = CLASS +" "+ id;

		Cursor c = model.db.query(Chats.TABLE, Chats.COLS, Chats.FOREIGN_ROOM+"="+id, null, null, null, Chats.KEY_ID +" DESC", "1");

		if (c.moveToNext()) {
			this.lastId =  c.getInt(0);
			c.close();

			c = model.db.query(Chats.TABLE, Chats.COLS, Chats.FOREIGN_ROOM+"="+id, null, null, null, Chats.KEY_TIME +" DESC", "1");
			if (c.moveToNext()) {
				this.lastUpdate = c.getLong(1);
			}
			c.close();
		}
		c.close();

		Log.i(THIS, "Chatroom restored: "+ id +", mostRecent:"+ lastId +", time:"+ lastUpdate);
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
	public void refresh() {
		if (!model.isConnected()) {
			err.notLoggedIn();
			return;
		}
		
		try {
			setChatStatus(ChatStatus.Refreshing);
			
			model.exec.execute(new JSONArrayRequestExecutor<>(model.factory.chatRefreshRequest(id, lastUpdate), new ChatEntry.ChatConverter(), this, Tasks.CHAT_REFRESH));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void chatRefreshResult(RequestExecutor<List<ChatEntry>, ?> r) {
		if (r.isSuccessful()){
			List<ChatEntry> res = r.getResult();
			
			model.getLogin().validated();

			if (res.size() > 0)
				putChats(res);
			else
				Log.i(THIS, "No new Entries");
			setChatStatus(ChatStatus.Ready);
		} else {
			err.asyncTaskResponseError(r.getException());
			setChatStatus(ChatStatus.Offline);
		}
	}

	@Override
	public void addChat(ChatEntry chatEntry) {
		ContentValues insert = new ContentValues();
		insert.put(Chats.KEY_ID, chatEntry.chatID);
		insert.put(Chats.KEY_MESSAGE, chatEntry.message);
		insert.put(Chats.KEY_TIME, chatEntry.timestamp);
		insert.put(Chats.KEY_PICTURE, chatEntry.pictureID);
		insert.put(Chats.FOREIGN_GROUP, chatEntry.groupID);
		insert.put(Chats.FOREIGN_USER, chatEntry.userID);
		insert.put(Chats.FOREIGN_ROOM, id);

		model.db.insert(Chats.TABLE, null, insert);

		List<ChatEntry> upd = new ArrayList<>();
		upd.add(chatEntry);

		notifyChatsEdited(upd);
	}

	@Override
	public void editChat(ChatEntry chatEntry) {
		ContentValues update = new ContentValues();
		update.put(Chats.KEY_MESSAGE, chatEntry.message);
		update.put(Chats.KEY_TIME, chatEntry.timestamp);
		update.put(Chats.KEY_PICTURE, chatEntry.pictureID);
		update.put(Chats.FOREIGN_GROUP, chatEntry.groupID);
		update.put(Chats.FOREIGN_USER, chatEntry.userID);
		update.put(Chats.FOREIGN_ROOM, id);

		model.db.update(Chats.TABLE, update, Chats.KEY_ID+"="+chatEntry.chatID, null);

		List<ChatEntry> upd = new ArrayList<>();
		upd.add(chatEntry);

		notifyChatsEdited(upd);
	}
	
	private void putChats(List<ChatEntry> entries) {
		SQLiteDatabase db = model.db;
		
		SQLiteStatement s = db.compileStatement("INSERT INTO "+ Chats.TABLE +
				" ("+ DatabaseHelper.strStr(Chats.COLS) +") VALUES (?, ?, "+ model.getLogin().getGroupID() +", ?, ?, ?, "+ id +")");
		
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
					s.bindLong(2, c.timestamp); //Timestamp
					s.bindLong(3, c.userID);
					s.bindString(4, c.message);
					s.bindLong(5, (c.pictureID != null)? c.pictureID : 0);
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
				lastUpdate = last.timestamp;
				lastId = last.chatID;
			}

		} catch (Exception e) {
			Log.e(THIS, "All Inserts failed", e);
		} finally {
			s.close();
		}
		
		Log.i(THIS, "Received "+ entries.size() +" new Chats in Chatroom "+ this.id +" since "+ this.lastUpdate +"");
		
		notifyChatsAdded(entries);
	}

	private void setChatStatus(final ChatStatus newStatus) {
		status = newStatus;
		
		Log.i(THIS, "Status: "+ newStatus);
		
		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IChatListener l: listeners) {
					l.onChatStatusChanged(newStatus);
				}
			}
		});
	}

	@Override
	public ChatStatus getChatStatus() {
		return status;
	}

	@Override
	public void addListener(IChatListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(IChatListener l) {
		listeners.remove(l);
	}

	/**
	 * Request a callback with all available Chats
	 * Callback pattern in anticipation of asynchronous DB access
	 * @param callback the single Listener that wants to completely refresh or initialize its content
	 */
	@Override
	public void provideChats(IChatListener callback) {
		Log.i(THIS, "Chats requested");
		callback.chatsProvided(getAllChats());
	}
	
	private void notifyChatsEdited(final List<ChatEntry> entries) {
		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for(IChatListener l: listeners) {
					l.chatsEdited(entries);
				}
			}
		});
	}
	
	private void notifyChatsAdded(final List<ChatEntry> entries) {
		Log.i(THIS, "Notifiying with "+ entries.size() +" entries");

		model.uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for(IChatListener l: listeners) {
					l.chatsAdded(entries);
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
	public void postChat(String msg, Integer pictureID) {
		//TODO: save to DB
		if (!model.isConnected()) {
            err.notLoggedIn();
            return;
        }
        try {
        	setChatStatus(ChatStatus.Posting);
        	model.exec.execute(new RequestExecutor<String, Tasks>(model.factory.chatPostRequest(id, msg, pictureID), null, this, Tasks.CHAT_POST));
        } catch (HttpRequestException e) {
                err.requestException(e);
        }
	}
	
	private void chatPostResult(RequestExecutor<String, ?> r) {
		if (r.isSuccessful()) {
			notifyChatsAdded(null);//TODO
			setChatStatus(ChatStatus.Ready);
		} else {
			setChatStatus(ChatStatus.Offline);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void executorResult(RequestExecutor<?, Tasks> r, Tasks callbackId) {
		switch (callbackId) {
		case CHAT_POST:
			chatPostResult((RequestExecutor<String, ?>) r);
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
