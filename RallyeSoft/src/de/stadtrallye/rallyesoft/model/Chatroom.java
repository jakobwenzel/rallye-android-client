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
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.Chatroom.Tasks;
import de.stadtrallye.rallyesoft.model.comm.Paths;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Chatrooms;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Chats;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Messages;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

public class Chatroom implements IChatroom, RequestExecutor.Callback<Tasks> {
	
	// statics
	final private static String CLASS = Chatroom.class.getSimpleName();
	final private String THIS;
	final private static ErrorHandling err = new ErrorHandling(CLASS);
	
	enum Tasks { CHAT_REFRESH, CHAT_POST };
	
	// members
	final private Model model;
	final private int id;
	final private String name;
	private int lastUpdate = 0;
	private int lastId = 0;
//	private long pendingLastTime = 0;
	
	private ChatStatus status;
	
	private ArrayList<IChatListener> listeners = new ArrayList<IChatListener>();
	
	/**
	 * Get Chatrooms from Database
	 * Requires: Previously logged in with identical login
	 * @param model
	 * @return
	 */
	static List<Chatroom> getChatrooms(Model model) {
		List<Chatroom> out = new ArrayList<Chatroom>();
		//TODO: currently Chatrooms can only ever belong to 1 group, and will not be shown, even if another group has rights to access
		Cursor c = model.db.query(Chatrooms.TABLE, new String[]{Chatrooms.KEY_ID, Chatrooms.KEY_NAME, Chatrooms.KEY_LAST_UPDATE, Chatrooms.KEY_LAST_ID}, Chatrooms.FOREIGN_GROUP+"="+model.getLogin().group, null, null, null, null);
		
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
	public Chatroom(int id, String name, Model model) {
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
		SQLiteDatabase db = model.db;
		
		Cursor answer = db.query(Chatrooms.TABLE, new String[]{Chatrooms.KEY_NAME}, Chatrooms.KEY_ID+"="+id, null, null, null, null);
		if (answer.getCount() < 1) {//TODO: check for different names? ensure consistency
		
			ContentValues insert = new ContentValues();
			insert.put(Chatrooms.KEY_ID, id);
			insert.put(Chatrooms.KEY_NAME, name);
			insert.put(Chatrooms.FOREIGN_GROUP, model.getLogin().group);
			insert.put(Chatrooms.KEY_LAST_UPDATE, 0);
			insert.put(Chatrooms.KEY_LAST_ID, lastId);
			db.insert(Chatrooms.TABLE, null, insert);
		}
		answer.close();
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
		if (!model.isConnected()) {
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
			setChatStatus(ChatStatus.Refreshing);
			
			model.exec.execute(new JSONArrayRequestExecutor<ChatEntry, Tasks>(model.factory.chatRefreshRequest(id, lastUpdate), new ChatEntry.ChatConverter(), this, Tasks.CHAT_REFRESH));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	private void chatRefreshResult(RequestExecutor<List<ChatEntry>, ?> r) {
		if (r.isSuccessful()){
			List<ChatEntry> res = r.getResult();
			
			model.getLogin().validated();
			
			chatUpdate(res);
			setChatStatus(ChatStatus.Ready);
		} else {
			err.asyncTaskResponseError(r.getException());
			setChatStatus(ChatStatus.Offline);
		}
	}
	
	private void chatUpdate(final List<ChatEntry> entries) {
		SQLiteDatabase db = model.db;
		
//		removeRedundantChats(entries, db);
		
		
		SQLiteStatement s = db.compileStatement("INSERT INTO "+ Chats.TABLE +
				" ("+ DatabaseHelper.strStr(Chats.COLS) +") VALUES (?, ?, "+ model.getLogin().group +", ?, ?, ?, "+ id +")");
		SQLiteStatement t = db.compileStatement("INSERT INTO "+ Messages.TABLE +
				" ("+ DatabaseHelper.strStr(Messages.COLS) +") VALUES (null, ?)");
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
		
		model.uiHandler.post(new Runnable(){
			@Override
			public void run() {
				for(IChatListener l: listeners) {
					l.addedChats(entries);
				}
			}
		});
	}

	private void setChatStatus(final ChatStatus newStatus) {
		status = newStatus;
		
		Log.i(THIS, "Status: "+ newStatus);
		
		model.uiHandler.post(new Runnable(){
			@Override
			public void run() {
				for (IChatListener l: listeners) {
					l.onChatStatusChanged(newStatus);
				}
			}
		});
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
		
		Cursor c = model.db.query(Chats.TABLE+" AS c LEFT JOIN "+Messages.TABLE+" AS m USING ("+Chats.FOREIGN_MSG+")",
				new String[]{Chats.KEY_ID, Messages.KEY_MSG, Chats.KEY_TIME, Chats.FOREIGN_GROUP, Chats.KEY_SELF, Chats.KEY_PICTURE},
				Chatrooms.KEY_ID+"="+id,
				null, null, null, Chats.KEY_TIME);
		
		ArrayList<ChatEntry> out = new ArrayList<ChatEntry>();
		
		while (c.moveToNext()) {
			out.add(new ChatEntry(c.getInt(0), c.getString(1), c.getInt(2), c.getInt(3), sqlToBool(c.getInt(4)), c.getInt(5)));
		}
		c.close();
		
		return out;
	}
	
	private class PictureGallery extends AbstractPictureGallery {
		
		public PictureGallery(int initialPictureID) {
			this.pictures = new ArrayList<Integer>();
			
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
			return Chatroom.this.getUrlFromImageId(this.pictures.get(pos), size.toChar());
		}
		
	}
	
	/**
	 * @param initialPictureId -1 results in a initialPosition of 0
	 */
	@Override
	public IPictureGallery getPictureGallery(int initialPictureId) {
		return new PictureGallery(initialPictureId);
	}
	
	@Override
	public String getUrlFromImageId(int pictureID, char size) {
		String res = model.getLogin().server + Paths.getPic(pictureID, size);
//		Log.v(THIS, res);
		return res;
	}
	
	@Override
	public void saveCurrentState(int lastRead) {
		
	}
	
	@Override
	public void addChat(String msg) {
		//TODO: save to DB
		if (!model.isConnected()) {
            err.notLoggedIn();
            return;
        }
        try {
        	setChatStatus(ChatStatus.Posting);
        	model.exec.execute(new RequestExecutor<String, Tasks>(model.factory.chatPostRequest(id, msg, 0), null, this, Tasks.CHAT_POST));
        } catch (HttpRequestException e) {
                err.requestException(e);
        }
	}
	
	private void addChatResult(RequestExecutor<String, ?> r) {//TODO prevent from Failing
//		if (r.isSuccessful()) {
//			setChatStatus(ChatStatus.PostSuccessfull);
//			setChatStatus(ChatStatus.Ready);
//		} else {
//			setChatStatus(ChatStatus.PostFailed);
//			setChatStatus(ChatStatus.Offline);
//		}
		setChatStatus(ChatStatus.PostSuccessfull);
		setChatStatus(ChatStatus.Ready);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void executorResult(RequestExecutor<?, Tasks> r, Tasks callbackId) {
		switch (callbackId) {
		case CHAT_POST:
			addChatResult((RequestExecutor<String, ?>) r);
			break;
		case CHAT_REFRESH:
			chatRefreshResult((RequestExecutor<List<ChatEntry>, ?>) r);
			break;
		default:
			Log.e(THIS, "Unknown Executor Callback");
			break;
		}
	}
	
	@Override
	public void onDestroy() {
		
		
	}
	
	@Override
	public void onStop() {
		
		
	}
}
