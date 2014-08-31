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

import android.database.Cursor;

import java.util.List;
import java.util.Map;

import de.rallye.model.structures.Submission;
import de.stadtrallye.rallyesoft.uimodel.IPicture;

/**
 * Models CallbackIds for a Rallye-type game
 *
 */
public interface ITasks {

	/**
	 * Refresh the CallbackIds from the server
	 */
	void refresh();

	void refreshSubmissions();

	void submitSolution(int taskID, int type, IPicture picture, String text, String number);

	Map<Integer, List<Submission>> getSubmissions();

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

	int getLocationSpecificTasksCount();

	/**
	 * Callbacks
	 */
	public interface ITasksListener {
		void taskUpdate();

		void submissionsUpdate(Map<Integer, List<Submission>> submissions);
	}
}
