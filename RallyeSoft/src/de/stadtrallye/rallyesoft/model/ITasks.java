package de.stadtrallye.rallyesoft.model;

import android.database.Cursor;

/**
 * Models CallbackIds for a Rallye-type game
 *
 */
public interface ITasks {

	/**
	 * Refresh the CallbackIds from the server
	 */
	void updateTasks();

	void addListener(ITasksListener l);
	void removeListener(ITasksListener l);

//	/**
//	 * Get all tasks that are contingent on a specific location
//	 * @return Cursor, containing: _id, CallbackIds.KEY_NAME ("name"), CallbackIds.KEY_DESCRIPTION ("description"), CallbackIds.KEY_LOCATION_SPECIFIC ("locationSpecific"), CallbackIds.KEY_LAT ("latitude"), CallbackIds.KEY_LON ("longitude"), CallbackIds.KEY_MULTIPLE ("multipleSubmits), CallbackIds.KEY_SUBMIT_TYPE ("submitType")
//	 */
//	@Deprecated
//	Cursor getLocationSpecificTasksCursor();

//	/**
//	 * Get all tasks that are not contingent on a specific location
//	 * @return Cursor, containing: _id, CallbackIds.KEY_NAME ("name"), CallbackIds.KEY_DESCRIPTION ("description"), CallbackIds.KEY_LOCATION_SPECIFIC ("locationSpecific"), CallbackIds.KEY_LAT ("latitude"), CallbackIds.KEY_LON ("longitude"), CallbackIds.KEY_MULTIPLE ("multipleSubmits), CallbackIds.KEY_SUBMIT_TYPE ("submitType")
//	 */
//	@Deprecated
//	Cursor getUbiquitousTasksCursor();

	/**
	 * Get all tasks, location specific tasks first
	 * @return Cursor, containing: _id, CallbackIds.KEY_NAME ("name"), CallbackIds.KEY_DESCRIPTION ("description"), CallbackIds.KEY_LOCATION_SPECIFIC ("locationSpecific"), CallbackIds.KEY_LAT ("latitude"), CallbackIds.KEY_LON ("longitude"), CallbackIds.KEY_MULTIPLE ("multipleSubmits), CallbackIds.KEY_SUBMIT_TYPE ("submitType")
	 */
	Cursor getTasksCursor();

	int getTaskPositionInCursor(int id);

	/**
	 * Callbacks
	 */
	public interface ITasksListener {
		void taskUpdate();
	}
}
