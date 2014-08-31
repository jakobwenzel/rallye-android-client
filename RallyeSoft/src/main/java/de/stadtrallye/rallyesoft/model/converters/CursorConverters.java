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

package de.stadtrallye.rallyesoft.model.converters;

import android.database.Cursor;

import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.LatLng;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.model.structures.Task;

import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Tasks;
import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.getBoolean;

/**
 * Functions to extract one Object from a Cursor, in case not everything can be done with a CursorAdapter
 * Also contains classes to index a cursor by column names, so the order can change
 */
public class CursorConverters {

	public static class TaskCursorIds {
		public int name, locationSpecific, description, latitude, longitude, multiple, submitType, id, radius, points, additionalResources, submits;

		/**
		 *
		 * @param cursor needs to contain certain rows
		 */
		public static TaskCursorIds read(Cursor cursor) {
			if (cursor == null)
				return null;

			TaskCursorIds c = new TaskCursorIds();

			c.name = cursor.getColumnIndexOrThrow(Tasks.KEY_NAME);
			c.description = cursor.getColumnIndexOrThrow(Tasks.KEY_DESCRIPTION);
			c.locationSpecific = cursor.getColumnIndexOrThrow(Tasks.KEY_LOCATION_SPECIFIC);
			c.latitude = cursor.getColumnIndexOrThrow(Tasks.KEY_LAT);
			c.longitude = cursor.getColumnIndexOrThrow(Tasks.KEY_LON);
			c.multiple = cursor.getColumnIndexOrThrow(Tasks.KEY_MULTIPLE);
			c.submitType = cursor.getColumnIndexOrThrow(Tasks.KEY_SUBMIT_TYPE);
			c.id = cursor.getColumnIndexOrThrow("_id");
			c.radius = cursor.getColumnIndexOrThrow(Tasks.KEY_RADIUS);
			c.points = cursor.getColumnIndexOrThrow(Tasks.KEY_POINTS);
			c.additionalResources = cursor.getColumnIndexOrThrow(Tasks.KEY_ADDITIONAL_RESOURCES);
			c.submits = cursor.getColumnIndexOrThrow(Tasks.KEY_SUBMITS);

			return c;
		}
	}

	public static Task getTask(Cursor cursor, TaskCursorIds c) {
		LatLng coords;

		coords = (cursor.isNull(c.latitude) || cursor.isNull(c.longitude))? null : new LatLng(cursor.getDouble(c.latitude), cursor.getDouble(c.longitude));

		return new Task(cursor.getInt(c.id), getBoolean(cursor, c.locationSpecific), coords,
				cursor.getDouble(c.radius), cursor.getString(c.name), cursor.getString(c.description),
				getBoolean(cursor, c.multiple), cursor.getInt(c.submitType), cursor.getString(c.points),
				AdditionalResource.additionalResourcesFromString(cursor.getString(c.additionalResources)),
				cursor.getInt(c.submits));
	}

	public static Task getTask(int pos, Cursor cursor, TaskCursorIds c) {
		cursor.moveToPosition(pos);

		return getTask(cursor,c);
	}

	public static ChatEntry getChatEntry(Cursor cursor, ChatCursorIds c) {
		return new ChatEntry(cursor.getInt(c.id),cursor.getString(c.message),cursor.getLong(c.timestamp),
				cursor.getInt(c.groupID),cursor.getString(c.groupName),cursor.getInt(c.userID),
				cursor.getString(c.userName),cursor.getInt(c.pictureID));
	}


	/**
	 * Move a Cursor to a id
	 * This assumes that the cursor is sorted by id
	 * @return true if the element could be found.
 	 */
	public static boolean moveCursorToId(Cursor cursor, int column, int id) {
		int left = 0;
		int right = cursor.getCount()-1;

		while (left <= right) {
			int middle = (left+right) / 2;

			cursor.moveToPosition(middle);
			int val = cursor.getInt(column);
			if (val==id)
				return true;
			if (val > id)
				right = middle-1;
			else
				left = middle +1;
		}

		return false;
	}

	public static class ChatCursorIds {
		public int groupID, userID, userName, groupName, timestamp, message, pictureID, id;

		/**
		 *
		 * @param cursor needs to contain certain rows
		 */
		public static ChatCursorIds read(Cursor cursor) {
			if (cursor == null)
				return null;

			ChatCursorIds c = new ChatCursorIds();

			c.groupID = cursor.getColumnIndexOrThrow(DatabaseHelper.Chats.FOREIGN_GROUP);
			c.userID = cursor.getColumnIndexOrThrow(DatabaseHelper.Chats.FOREIGN_USER);
			c.userName = cursor.getColumnIndexOrThrow(DatabaseHelper.Users.KEY_NAME);
			c.groupName = cursor.getColumnIndexOrThrow(DatabaseHelper.Groups.KEY_NAME);
			c.timestamp = cursor.getColumnIndexOrThrow(DatabaseHelper.Chats.KEY_TIME);
			c.message = cursor.getColumnIndexOrThrow(DatabaseHelper.Chats.KEY_MESSAGE);
			c.pictureID = cursor.getColumnIndexOrThrow(DatabaseHelper.Chats.KEY_PICTURE);
			c.id = cursor.getColumnIndexOrThrow("_id");

			return c;
		}
	}
}
