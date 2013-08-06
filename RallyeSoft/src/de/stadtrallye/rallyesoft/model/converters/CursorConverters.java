package de.stadtrallye.rallyesoft.model.converters;

import android.database.Cursor;

import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.Task;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;

import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.getBoolean;

/**
 * Created by Ramon on 06.08.13.
 */
public class CursorConverters {

	public static class TaskCursorIds {
		public int name, locationSpecific, description, latitude, longitude, multiple, submitType, id;

		/**
		 *
		 * @param cursor needs to contain certain rows
		 */
		public static TaskCursorIds read(Cursor cursor) {
			if (cursor == null)
				return null;

			TaskCursorIds c = new TaskCursorIds();

			c.name = cursor.getColumnIndexOrThrow(DatabaseHelper.Tasks.KEY_NAME);
			c.description = cursor.getColumnIndexOrThrow(DatabaseHelper.Tasks.KEY_DESCRIPTION);
			c.locationSpecific = cursor.getColumnIndexOrThrow(DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC);
			c.latitude = cursor.getColumnIndexOrThrow(DatabaseHelper.Tasks.KEY_LAT);
			c.longitude = cursor.getColumnIndexOrThrow(DatabaseHelper.Tasks.KEY_LON);
			c.multiple = cursor.getColumnIndexOrThrow(DatabaseHelper.Tasks.KEY_MULTIPLE);
			c.submitType = cursor.getColumnIndexOrThrow(DatabaseHelper.Tasks.KEY_SUBMIT_TYPE);
			c.id = cursor.getColumnIndexOrThrow("_id");

			return c;
		}
	}

	public static Task getTask(int pos, Cursor cursor, TaskCursorIds c) {
		cursor.moveToPosition(pos);

		LatLng coords;

		coords = (cursor.isNull(c.latitude) || cursor.isNull(c.longitude))? null : new LatLng(cursor.getDouble(c.latitude), cursor.getDouble(c.longitude));

		return new Task(cursor.getInt(c.id), getBoolean(cursor, c.locationSpecific), coords, cursor.getString(c.name), cursor.getString(c.description), getBoolean(cursor, c.multiple), cursor.getInt(c.submitType));
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
