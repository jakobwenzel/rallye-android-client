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
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.model.tasks;

import android.database.Cursor;
import android.location.Location;

import de.rallye.model.structures.PushPrimarySubmissionConfig;
import de.rallye.model.structures.PushSubmission;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.IHandlerCallback;
import de.stadtrallye.rallyesoft.model.pictures.IPictureManager;

/**
 * Models CallbackIds for a Rallye-type game
 *
 */
public interface ITaskManager {

	/**
	 * Refresh the CallbackIds from the server
	 */
	void update() throws NoServerKnownException;

	/**
	 * Force refresh
	 */
	void forceRefresh() throws NoServerKnownException;

	ITask getTask(int taskID);

	//void refreshSubmissions();

	ITask getTaskFromCursor(Cursor cursor);

	void submitSolution(int taskID, int type, IPictureManager.IPicture picture, String text, Integer number, Location lastLocation) throws NoServerKnownException;

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

	int getLocationSpecificTasksCount();

	void pushSubmission(PushSubmission submission);

	void pushActiveSubmission(PushPrimarySubmissionConfig primary);

	/**
	 * Callbacks
	 */
	public interface ITasksListener extends IHandlerCallback {
		void taskUpdate();

		//void submissionsUpdate(Map<Integer, List<Submission>> submissions);
	}
}
