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
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.net.Server;
import de.stadtrallye.rallyesoft.net.retrofit.RetroAuthCommunicator;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_CHATS;

/**
 * Created by Ramon on 22.09.2014.
 */
public class ChatManager implements IChatManager {

	private static final String THIS = ChatManager.class.getSimpleName();

	private final SQLiteDatabase db;
	private RetroAuthCommunicator communicator;
	private List<Chatroom> chatrooms;
	private final ReadWriteLock chatroomLock = new ReentrantReadWriteLock();
	private final List<IChatListener> listeners = new ArrayList<>();

	public ChatManager(RetroAuthCommunicator communicator, SQLiteDatabase db) {
		this.db = db;
		this.communicator = communicator;
	}

	public ChatManager() throws NoServerKnownException {
		this(Server.getCurrentServer().getAuthCommunicator(), Storage.getDatabase());
	}

	@Override
	public boolean isChatReady() {
		chatroomLock.readLock().lock();
		try {
			return chatrooms != null;
		} finally {
			chatroomLock.readLock().unlock();
		}
	}

	@Override
	public List<? extends IChatroom> getChatrooms() {
		chatroomLock.readLock().lock();
		try {
			return Collections.unmodifiableList(chatrooms);
		} finally {
			chatroomLock.readLock().unlock();
		}
	}

	@Override
	public IChatroom findChatroom(int roomID) {
		chatroomLock.readLock().lock();
		try {
			if (chatrooms == null)
				return null;

			for (IChatroom r : chatrooms) {
				if (r.getID() == roomID) {
					return r;
				}
			}

			return null;
		} finally {
			chatroomLock.readLock().unlock();
		}
	}

	@Override
	public void updateChatrooms() {
		checkServerKnown();

		communicator.getAvailableChatrooms(new Callback<List<Chatroom>>() {
			@Override
			public void success(List<Chatroom> chatrooms, Response response) {
				chatroomLock.writeLock().lock();
				try {
					ChatManager.this.chatrooms = chatrooms;
				} finally {
					chatroomLock.writeLock().unlock();
				}
				notifyChatroomsChanged();
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Update of available Chatrooms failed", e);
				//TODO Server.getServer().commFailed(e);
			}
		});
	}

	private void checkServerKnown() throws NoServerKnownException{
		if (communicator == null) {
			throw new NoServerKnownException();
		}
	}

	@Override
	public void forceRefreshChatrooms() {
		db.delete(DatabaseHelper.Chatrooms.TABLE, null, null);

		chatroomLock.writeLock().lock();
		try {
			chatrooms = null;
		} finally {
			chatroomLock.writeLock().unlock();
		}

		updateChatrooms();
	}

	@Override
	public void addListener(IChatListener chatListener) {
		synchronized (listeners) {
			listeners.add(chatListener);
		}
	}

	@Override
	public void removeListener(IChatListener chatListener) {
		synchronized (listeners) {
			listeners.remove(chatListener);
		}
	}

	public void readChatrooms() {
		List<Chatroom> out = new ArrayList<>();

		Cursor c = db.query(DatabaseHelper.Chatrooms.TABLE, DatabaseHelper.Chatrooms.COLS, null, null, null, null, null);

		while (c.moveToNext()) {
			Chatroom room = new Chatroom(c.getInt(0), c.getString(1), c.getInt(3), c.getLong(2));

			if (Storage.hasStructureChanged(EDIT_CHATS)) {
				room.forceRefresh();
			}

			out.add(room);
		}

		Storage.structureChangeHandled(EDIT_CHATS);

		Log.i(THIS, "Read " + out.size() + " Chatrooms");
		chatroomLock.writeLock().lock();
		try {
			chatrooms = out;
		} finally {
			chatroomLock.writeLock().unlock();
		}
	}

	public void writeChatrooms() {
		chatroomLock.readLock().lock();
		try {
			if (chatrooms == null)
				return;

			db.delete(DatabaseHelper.Chatrooms.TABLE, null, null);

			for (Chatroom c : chatrooms) {
				ContentValues insert = new ContentValues();
				c.fillRoomContentValues(insert);
				db.insert(DatabaseHelper.Chatrooms.TABLE, null, insert);
			}
		} finally {
			chatroomLock.readLock().unlock();
		}
	}

	private void notifyChatroomsChanged() {
		synchronized (listeners) {
			Handler handler;
			for (final IChatListener l : listeners) {
				handler = l.getCallbackHandler();
				if (handler == null) {
					l.onChatroomsChange();
				} else {
					handler.post(new Runnable() {
						@Override
						public void run() {
							l.onChatroomsChange();
						}
					});
				}
			}
		}
	}
}
