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
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.net.Server;
import de.stadtrallye.rallyesoft.net.retrofit.RetroAuthCommunicator;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper;

import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_CHATS;

/**
 * Created by Ramon on 22.09.2014.
 */
public class ChatManager implements IChatManager {

	private static final String THIS = ChatManager.class.getSimpleName();

	private final SQLiteDatabase db;
	private final RetroAuthCommunicator communicator;
	private List<Chatroom> chatrooms;
	private final List<IChatListener> listeners = new ArrayList<>();

	public ChatManager() {
		this.communicator = Server.getCurrentServer().getAuthCommunicator();
		this.db = Storage.getDatabase();
	}

	@Override
	public boolean isChatReady() {
		return chatrooms != null;
	}

	@Override
	public List<? extends IChatroom> getChatrooms() {
		return Collections.unmodifiableList(chatrooms);
	}

	@Override
	public void forceRefreshChatrooms() {

	}

	@Override
	public void addListener(IChatListener chatListener) {
		listeners.add(chatListener);
	}

	@Override
	public void removeListener(IChatListener chatListener) {
		listeners.remove(chatListener);
	}

	public void readChatrooms() {
		List<Chatroom> out = new ArrayList<>();

		Cursor c = db.query(DatabaseHelper.Chatrooms.TABLE, DatabaseHelper.Chatrooms.COLS, null, null, null, null, null);

		while (c.moveToNext()) {
			Chatroom room = new Chatroom(c.getInt(0), c.getString(1), c.getInt(3), c.getLong(2));

			if (Storage.hasStructureChanged(EDIT_CHATS)) {
				try {
					room.forceRefresh();
				} catch (NoServerKnownException e) {
					Log.e(THIS, "Could not forceRefresh a Chatroom (Chatroom refused)", e);
				}
			}

			out.add(room);
		}

		Storage.structureChangeHandled(EDIT_CHATS);

		Log.i(THIS, "Read " + out.size() + " Chatrooms");
		chatrooms = out;
	}

	public void writeChatrooms() {
		if (chatrooms == null)
			return;

		db.delete(DatabaseHelper.Chatrooms.TABLE, null, null);

		for (Chatroom c : chatrooms) {
			ContentValues insert = new ContentValues();
			c.fillRoomContentValues(insert);
			db.insert(DatabaseHelper.Chatrooms.TABLE, null, insert);
		}
	}

	private void notifyChatroomsChanged() {
		for (IChatListener l : listeners) {
			l.onChatroomsChange();
		}
	}
}
