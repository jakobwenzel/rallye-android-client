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

package de.stadtrallye.rallyesoft.services;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import de.rallye.model.structures.Chatroom;
import de.rallye.model.structures.PushChatEntry;
import de.rallye.model.structures.PushEntity;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.chat.IChatroom;
import de.stadtrallye.rallyesoft.model.converters.JsonConverters;
import de.stadtrallye.rallyesoft.model.converters.Serialization;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.net.Server;
import de.wirsch.gcm.GcmBaseIntentService;

public class GcmIntentService extends GcmBaseIntentService {
	
	private static final String THIS = GcmIntentService.class.getSimpleName();

	@Override
	protected void onMessage(Bundle message) {
		Log.i(THIS, "Received Push: " +message);
		Context context = getApplicationContext();

		try {
			PushEntity.Type type = PushEntity.Type.valueOf(message.getString(PushEntity.TYPE));
			ObjectMapper mapper = Serialization.getInstance();

			switch (type) {
				case newMessage:
					PushChatEntry chat = mapper.readValue(message.getString(PushEntity.PAYLOAD), PushChatEntry.class);
					IChatroom chatroom = Server.getCurrentServer().getChat().findChatroom(chat.roomID);
					chatroom.pushChat(chat.entry);
					break;
				case messageChanged:
					chat = mapper.readValue(message.getString(PushEntity.PAYLOAD), PushChatEntry.class);
					chatroom = Server.getCurrentServer().getChat().findChatroom(chat.roomID);
					chatroom.changedChat(chat.entry);
					break;
				default:
			}
		} catch (JSONException | IOException e) {
			Log.e(THIS, "Push Message not compatible", e);
		}
	}

	@Override
	protected void onError(String errorId) {
		Log.e(THIS, "Error: "+ errorId);
	}

	@Override
	protected void onDeleted(String message) {
		Log.e(THIS, "Deleted: "+ message);
	}

	/*@Override
	protected void onRegistered(Context context, String registrationId) {
		Log.i(THIS, "Registered GCM!");
		
		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Log.w(THIS, "Unregistered GCM!");
//		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
		
		Model.getInstance(getApplicationContext()).logout();
	}*/

}