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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import de.rallye.model.structures.PushChatEntry;
import de.rallye.model.structures.PushEntity;
import de.rallye.model.structures.PushPrimarySubmissionConfig;
import de.rallye.model.structures.PushSubmission;
import de.stadtrallye.rallyesoft.geolocation.LocationManager;
import de.stadtrallye.rallyesoft.model.AndroidNotificationManager;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.chat.ChatEntry;
import de.stadtrallye.rallyesoft.model.chat.IChatManager;
import de.stadtrallye.rallyesoft.model.chat.IChatroom;
import de.stadtrallye.rallyesoft.model.tasks.ITaskManager;
import de.stadtrallye.rallyesoft.util.converters.Serialization;
import de.wirsch.gcm.GcmBaseIntentService;

public class GcmIntentService extends GcmBaseIntentService {
	
	private static final String THIS = GcmIntentService.class.getSimpleName();

	private IChatManager chatManager;
	private AndroidNotificationManager notificationManager;
	private ITaskManager taskManager;

	@Override
	public void onCreate() {
		super.onCreate();

		chatManager = Server.getCurrentServer().acquireChatManager(this);
		taskManager = Server.getCurrentServer().acquireTaskManager(this);
		notificationManager = AndroidNotificationManager.getInstance(getApplicationContext());
	}

	@Override
	protected void onMessage(Bundle message) {
		Log.i(THIS, "Received Push: " +message);
		Context context = getApplicationContext();

		try {
			PushEntity.Type type = PushEntity.Type.valueOf(message.getString(PushEntity.TYPE));
			ObjectMapper mapper = Serialization.getJsonInstance();

			switch (type) {
				case newMessage:
					PushChatEntry<ChatEntry> chat = mapper.readValue(message.getString(PushEntity.PAYLOAD), new TypeReference<PushChatEntry<ChatEntry>>(){});
					IChatroom chatroom = chatManager.findChatroom(chat.roomID);
					chatroom.pushChat(chat.entry, notificationManager);
					break;
				case messageChanged:
					chat = mapper.readValue(message.getString(PushEntity.PAYLOAD), new TypeReference<PushChatEntry<ChatEntry>>(){});
					chatroom = chatManager.findChatroom(chat.roomID);
					chatroom.editChat(chat.entry);
					break;
				case newSubmission:
					PushSubmission submission = mapper.readValue(message.getString(PushEntity.PAYLOAD), PushSubmission.class);
					taskManager.pushSubmission(submission);
					break;
				case setPrimarySubmission:
					PushPrimarySubmissionConfig primary = mapper.readValue(message.getString(PushEntity.PAYLOAD), PushPrimarySubmissionConfig.class);
					taskManager.pushActiveSubmission(primary);
				case refreshTasks:
					taskManager.forceRefresh();
					break;
				case refreshChatrooms:
					chatManager.forceRefreshChatrooms();
					break;
				case pingLocation:
					new LocationManager(this, true);
					break;
				default:
			}
		} catch (IOException e) {
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

	@Override
	public void onDestroy() {
		Server.getCurrentServer().releaseChatManager(this);
		Server.getCurrentServer().releaseTaskManager(this);

		super.onDestroy();
	}
}