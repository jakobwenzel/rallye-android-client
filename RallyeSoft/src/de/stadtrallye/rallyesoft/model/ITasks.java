package de.stadtrallye.rallyesoft.model;

import android.database.Cursor;

import de.rallye.model.structures.LatLng;

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

	/**
	 * Get all tasks that are contingent on a specific location
	 * @return Cursor, containing: _id, CallbackIds.KEY_NAME ("name"), CallbackIds.KEY_DESCRIPTION ("description"), CallbackIds.KEY_LOCATION_SPECIFIC ("locationSpecific"), CallbackIds.KEY_LAT ("latitude"), CallbackIds.KEY_LON ("longitude"), CallbackIds.KEY_MULTIPLE ("multipleSubmits), CallbackIds.KEY_SUBMIT_TYPE ("submitType")
	 */
	Cursor getLocationSpecificTasksCursor();

	/**
	 * Get all tasks that are not contingent on a specific location
	 * @return Cursor, containing: _id, CallbackIds.KEY_NAME ("name"), CallbackIds.KEY_DESCRIPTION ("description"), CallbackIds.KEY_LOCATION_SPECIFIC ("locationSpecific"), CallbackIds.KEY_LAT ("latitude"), CallbackIds.KEY_LON ("longitude"), CallbackIds.KEY_MULTIPLE ("multipleSubmits), CallbackIds.KEY_SUBMIT_TYPE ("submitType")
	 */
	Cursor getUbiquitousTasksCursor();

	//TODO
	Cursor getTasksCursor();

	/**
	 * Get the center of the game on the map (a Google Map can only be initialized with 1 set of coordinates at this time)
	 * @return Custom LatLng, modeled after Google Play Services LatLng, because it is closed source and needed on the server side (convert with LatLngAdapter.toGms())
	 */
	@Deprecated
	LatLng getMapLocation();

	/**
	 * Get a zoom level applicable to the coordinates from getMapLocation()
	 * @return Google Play Services / Google Map compatible
	 */
	@Deprecated
	float getZoomLevel();

	/**
	 * Callbacks
	 */
	public interface ITasksListener {
		void taskUpdate();
	}
}
