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
 */

package de.stadtrallye.rallyesoft.model;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.Html;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stadtrallye.rallyesoft.MainActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

/**
 * Created by wilson on 04.10.13.
 */
public class ChatNotificationManager {



	NotificationManager notificationService;
	private Context context;
	public ChatNotificationManager(Context context) {
		this.context = context;
		notificationService = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	/*public void newMessage(Chatroom room, ChatEntry entry) {
		List<ChatEntry> list = notifyingEntries.get(room);
		if (list==null) {
			list = new ArrayList<ChatEntry>();
			notifyingEntries.put(room,list);
		}

		list.add(entry);

		updateNotification();
	}*/

	protected void updateNotification() {

		Map<IChatroom,List<ChatEntry>> notifyingEntries = new HashMap<IChatroom,List<ChatEntry>>();
		List<? extends IChatroom> rooms = Model.getInstance(context).getChatrooms();
		for (IChatroom room: rooms) {
			List<ChatEntry> unread = room.getUnreadEntries();
			if (unread.size()>0)
				notifyingEntries.put(room,unread);
		}

		if (notifyingEntries.size()>0) {

			String heading, summary;
			int count;
			NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
			//Notification for one chatroom
			if (notifyingEntries.size()==1) {
				IChatroom room = notifyingEntries.keySet().iterator().next();
				List<ChatEntry> entries = notifyingEntries.get(room);

				Intent intent = new Intent(context, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra(Std.TAB, Std.CHATROOM);
				intent.putExtra(Std.CHATROOM, room.getID());
				intent.putExtra(Std.CHAT_ID, entries.get(0).chatID);
				PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

				heading = context.getString(R.string.new_messages_in) +" "+ room.getName();

				NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
				for (ChatEntry chatEntry : entries) {

					inboxStyle.addLine(Html.fromHtml("<b>"+chatEntry.getUserName()+" ("+chatEntry.getGroupName()+"):</b> "+chatEntry.message));
				}
				count = entries.size();

				summary = count+" "+context.getString(R.string.x_new_messages);

				builder
						.setStyle(inboxStyle)
						.setContentIntent(pendingIntent);

			} else { //Multiple Chatrooms
				Intent intent = new Intent(context, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra(Std.TAB, Std.CHATROOM);
				PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

				count = 0;
				heading = context.getString(R.string.new_messages);

				NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
				for (IChatroom room : notifyingEntries.keySet()) {
					int size = notifyingEntries.get(room).size();
					inboxStyle.addLine(room.getName()+" ("+size+")");
					count += size;
				}


				summary = context.getString(R.string.x_new_messages_in_y_chatrooms,count,notifyingEntries.size());



				builder.setContentIntent(pendingIntent).setStyle(inboxStyle);

			}

			builder.setNumber(count)
					.setSmallIcon(android.R.drawable.stat_notify_chat)
					.setContentTitle(heading)
					.setSubText(summary)
                    .setLights(Color.CYAN, 500, 500);

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(alarmSound);

			notificationService.notify("chat notification", Std.CHAT_NOTIFICATION, builder.build());
		} else
			notificationService.cancel("chat notification", Std.CHAT_NOTIFICATION);
	}

	/*public void setSeen(Chatroom room, ChatEntry entry) {

		List<ChatEntry> list = notifyingEntries.get(room);
		if (list!=null) {

			list.remove(entry);
			if (list.isEmpty())
				notifyingEntries.remove(room);

		}
		updateNotification();
	}*/



	static ChatNotificationManager instance;
	public static ChatNotificationManager getInstance(Context context) {
		if (instance==null)
			instance = new ChatNotificationManager(context);

		return instance;
	}

}
