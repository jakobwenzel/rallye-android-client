package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.Model.Tasks;
import de.stadtrallye.rallyesoft.model.backend.DatabaseOpenHelper.Chatrooms;
import de.stadtrallye.rallyesoft.model.backend.DatabaseOpenHelper.Chats;
import de.stadtrallye.rallyesoft.model.backend.DatabaseOpenHelper.Messages;
import de.stadtrallye.rallyesoft.model.comm.AsyncRequest;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayConverter;

public class Chatroom implements IChatroom, IAsyncFinished {
	
	// statics
	private static final String CLASS = Chatroom.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(CLASS);
	
	// members
	private Model model;
	private int id;
	private String name;
	private int lastUpdate = 0;
	private int lastId = 0;
//	private long pendingLastTime = 0;
	
	private final String THIS;
	
	private List<IChatListener> listeners = new ArrayList<IChatListener>();
	private ChatStatus status;
	
	
	static List<Chatroom> getChatrooms(Model model) {
		List<Chatroom> out = new ArrayList<Chatroom>();
		//TODO: currently Chatrooms can only ever belong to 1 group, and will not be shown, even if another group has rights to access
		Cursor c = model.getDb().query(Chatrooms.TABLE, new String[]{Chatrooms.KEY_ID, Chatrooms.KEY_NAME, Chatrooms.KEY_LAST_UPDATE, Chatrooms.KEY_LAST_ID}, Chatrooms.FOREIGN_GROUP+"="+model.getLogin().getGroup(), null, null, null, null);
		
		while (c.moveToNext()) {
			out.add(new Chatroom(c.getInt(0), c.getString(1), c.getInt(2), c.getInt(3), model));
		}
		
		return out;
	}
	
	private Chatroom(int id, String name, int lastTime, int lastId, Model model) {
		this(id, name, model);
		this.lastUpdate = lastTime;
		this.lastId = lastId;
	}
	
	/**
	 * 
	 * @param id ChatroomID
	 * @param name Name of the Chatroom
	 * @param model needed for access to current group, DB, etc.
	 * @param writeDb wether to ensure that this Chatroom is in the DB
	 */
	Chatroom(int id, String name, Model model) {
		this.id = id;
		this.name = name;
		this.model = model;
		
		THIS = CLASS +" "+ id;
	}
	
	/**
	 * Writes the "header" to DB
	 * Only the Chatroom information is written, not the Chats themselves (they will be written automatically on change)
	 * In order to write the information, the Groups must be written to the DB already
	 * Currently trying refresh() will fail if writeToDb() has not been called at least once on this DB
	 * 
	 * Method will not overwrite / update existing entries, so if the Server is changed, the table needs to be cleared!
	 */
	public void writeToDb() {
		SQLiteDatabase db = model.getDb();
		
		Cursor answer = db.query(Chatrooms.TABLE, new String[]{Chatrooms.KEY_NAME}, Chatrooms.KEY_ID+"="+id, null, null, null, null);
		if (answer.getCount() < 1) {//TODO: check for different names? ensure consistency
		
			ContentValues insert = new ContentValues();
			insert.put(Chatrooms.KEY_ID, id);
			insert.put(Chatrooms.KEY_NAME, name);
			insert.put(Chatrooms.FOREIGN_GROUP, model.getLogin().getGroup());
			insert.put(Chatrooms.KEY_LAST_UPDATE, 0);
			insert.put(Chatrooms.KEY_LAST_ID, lastId);
			db.insert(Chatrooms.TABLE, null, insert);
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
	public void refresh() {
		if (!model.isLoggedIn()) {
			err.notLoggedIn();
			return;
		}
		
//		long now = System.currentTimeMillis();
//		Log.i(THIS, "Refreshing: pending:"+ pendingLastTime +" now:"+ now);
		
		try {
//			if (pendingLastTime != 0) {
//				Log.w(THIS, "Already refreshing Chat");
//				return;
//			}
//			pendingLastTime = now;
			model.startAsyncTask(this, Tasks.CHAT_REFRESH, //TODO: Refresh
					model.getRallyePull().pendingChatRefresh(id, lastUpdate),
					new StringedJSONArrayConverter<ChatEntry>(new ChatEntry.ChatConverter()));
			
			chatStatusChange(ChatStatus.Refreshing);
			
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	private static String strStr(String... strings) {
		StringBuilder b = new StringBuilder();
		
		int l = strings.length-1;
		for (int i=0; i<=l; i++) {
			b.append(strings[i]);
			if (i < l)
				b.append(", ");
		}
		
		return b.toString();
	}
	
	private static final String CHATS_COLS = strStr(Chats.KEY_ID, Chats.KEY_TIME, Chats.FOREIGN_GROUP, Chats.KEY_SELF, Chats.FOREIGN_MSG, Chats.KEY_PICTURE, Chats.FOREIGN_ROOM);
	private static final String MSG_COLS = strStr(Messages.KEY_ID, Messages.KEY_MSG);
	
	private void chatUpdate(List<ChatEntry> entries) {//TODO: last entry will be add every refresh
		SQLiteDatabase db = model.getDb();
		
//		removeRedundantChats(entries, db);
		
		
		SQLiteStatement s = db.compileStatement("INSERT INTO "+ Chats.TABLE +
				" ("+ CHATS_COLS +") VALUES (?, ?, "+ model.getLogin().getGroup() +", ?, ?, ?, "+ id +")");
		SQLiteStatement t = db.compileStatement("INSERT INTO "+ Messages.TABLE +
				" ("+ MSG_COLS +") VALUES (null, ?)");
		SQLiteStatement u = db.compileStatement("SELECT COUNT(*) FROM "+ Chats.TABLE +" WHERE "+ Chats.KEY_ID +"=?");
		
		int msgId = -1, chatId = -1;
		int eliminated = 0;
		
		db.beginTransaction();
		try {
			ChatEntry c;
			for (Iterator<ChatEntry> i = entries.iterator(); i.hasNext();) {
				c = i.next();
				
				if (c.chatID <= lastId && true) {
					u.bindLong(1, c.chatID);
					if (u.simpleQueryForLong() > 0) {
						eliminated++;
						i.remove();
						continue;
					}
				}
				
				db.beginTransaction();
				try {
					
					if (c.message != null) {
						t.bindString(1, c.message); //Message
						msgId = (int) t.executeInsert();
					}
	//				Log.d(THIS, "Inserted "+c+" in Messages");
					
					s.bindLong(1, c.chatID);
					s.bindLong(2, c.timestamp); //Timestamp
					s.bindLong(3, c.self? 1 : 0); // self (0=false, 1=true)
					if (c.message != null)
						s.bindLong(4, msgId); //Message ID
					else
						s.bindNull(4);
					s.bindLong(5, c.pictureID); //Picture ID
					chatId = (int) s.executeInsert();
					
	//				Log.d(THIS, "Inserted "+c+" in Chats");
					
					if (chatId >= 0)
						db.setTransactionSuccessful();
					else
						err.dbInsertError(c.toString());
					
				} catch (Exception e) {
					Log.e(THIS, "Single Insert failed", e);
				} finally {
					db.endTransaction();
				}
			}
			
			if (entries.size() > 0) {
				ChatEntry last = entries.get(entries.size()-1);
				lastUpdate = last.timestamp;
				lastId = last.chatID;
				ContentValues update = new ContentValues();
				update.put(Chatrooms.KEY_LAST_UPDATE, lastUpdate);
				update.put(Chatrooms.KEY_LAST_ID, lastId);
				db.update(Chatrooms.TABLE, update, Chatrooms.KEY_ID+"="+id, null);
			}
			
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e(THIS, "All Inserts failed", e);
		} finally {
			s.close();
			t.close();
			db.endTransaction();
		}
		
		Log.i(THIS, "Received "+ entries.size() +" new Chats in Chatroom "+ this.id +" (since "+ this.lastUpdate +")  (eliminated "+ eliminated +")");
		
		for (IChatListener l: listeners) {
			l.addedChats(entries);
		}
	}
	
//	private void removeRedundantChats(List<ChatEntry> entries, SQLiteDatabase db) {
//		int i = entries.size()-1;
//		ChatEntry mine = entries.get(i);
//		ChatEntry theirs;
//		int last = mine.timestamp;
//		if (last == lastUpdate) {
//			
//			Cursor c = db.query(Chats.TABLE+" LEFT JOIN "+Messages.TABLE+" USING ("+Chats.FOREIGN_MSG+")", new String[]{Messages.KEY_MSG, Chats.KEY_TIME, Chats.FOREIGN_GROUP, Chats.KEY_SELF, Chats.KEY_PICTURE}, null, null, null, null, Chats.KEY_TIME+" DESC");
//			
//			
//			do {
//				theirs = new ChatEntry(c.getString(0), c.getInt(1), c.getInt(2), sqlToBool(c.getInt(3)), c.getInt(4));
//				mine = null;
//				if (theirs.equals(mine))
//					entries.remove(mine);
//				i--;
//			} while (mine.timestamp == theirs.timestamp);
//		}
//	}

	private void chatStatusChange(ChatStatus newStatus) {
		status = newStatus;
		
		Log.i(THIS, "Status: "+ newStatus);
		
		for (IChatListener l: listeners) {
			l.onChatStatusChanged(newStatus);
		}
	}
	
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
	
	private boolean sqlToBool(int in) {
		return (in > 0)? true : false;
	}

	@Override
	public List<ChatEntry> getAllChats() {
		
		Cursor c = model.getDb().query(Chats.TABLE+" AS c LEFT JOIN "+Messages.TABLE+" AS m USING ("+Chats.FOREIGN_MSG+")",
				new String[]{Chats.KEY_ID, Messages.KEY_MSG, Chats.KEY_TIME, Chats.FOREIGN_GROUP, Chats.KEY_SELF, Chats.KEY_PICTURE},
				Chatrooms.KEY_ID+"="+id,
				null, null, null, Chats.KEY_TIME);
		
		ArrayList<ChatEntry> out = new ArrayList<ChatEntry>();
		
		while (c.moveToNext()) {
			out.add(new ChatEntry(c.getInt(0), c.getString(1), c.getInt(2), c.getInt(3), sqlToBool(c.getInt(4)), c.getInt(5)));
		}
		
		return out;
	}
	
	@Override
	public void saveCurrentState(int lastRead) {
		
	}
	
	@Override
	public void addChat(String msg) {
		//TODO: save to DB
		if (!model.isLoggedIn()) {
            err.notLoggedIn();
            return;
        }
        try {
                model.startAsyncTask(this, Tasks.CHAT_POST, model.getRallyePull().pendingChatPost(id, msg, 0), null);
                chatStatusChange(ChatStatus.Posting);
        } catch (RestException e) {
                err.restError(e);
        }
	}

	@Override
	public void onAsyncFinished(@SuppressWarnings("rawtypes") AsyncRequest request, boolean success) {
		Tasks type = model.getRunningRequests().get(request);
		
		if (type == null && !success) {
			Log.w(THIS, "Task Callback with type 'null' => cancelled Task");
			return;
		} else if (type == null) {
			Log.e(THIS, "Task Callback with type 'null'");
			return;
		}
		
		switch (type) {
		case CHAT_REFRESH:
			try {
				if (success){
					List<ChatEntry> res = ((AsyncRequest<List<ChatEntry>>)request).get();
					
					model.getLogin().validated();
					
					chatUpdate(res);
					chatStatusChange(ChatStatus.Ready);
				} else {
					err.asyncTaskResponseError(request.getException());
					chatStatusChange(ChatStatus.Offline);
				}
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
			break;
		case CHAT_POST:
			if (success) {
				chatStatusChange(ChatStatus.Ready);
			} else {
				chatStatusChange(ChatStatus.Offline);
			}
			break;
		default:
			Log.e(THIS, "Unknown Task callback: "+ request);
		}
		
		model.getRunningRequests().remove(request);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		
	}
}
