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

package de.stadtrallye.rallyesoft.model.tasks;

import java.util.List;

import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.Submission;
import de.stadtrallye.rallyesoft.model.IHandlerCallback;

/**
 * Created by Ramon on 23.09.2014.
 */
public interface ITask {

	LatLng getLocation();

	void addListener(ITaskListener listener);
	void removeListener(ITaskListener listener);

	void updateSubmissions();

	String getName();
	String getDescription();

	List<AdditionalResource> getAdditionalResources();

	int getSubmitType();

	int getTaskID();

	/**
	 * Get as List of all submissions if they are already known
	 * @return null if not yet known
	 */
	List<Submission> getSubmissionsCached();

	/**
	 * Get cached submissions, or read them from Database
	 * @return always a list, can be empty
	 */
	List<Submission> getSubmissions();

	boolean hasLocation();

	double getRadius();

	boolean setPrimarySubmission(Submission submission);

	interface ITaskListener extends IHandlerCallback {
		void onSubmissionsChanged(List<Submission> submissions);
		void onTaskChange();
	}
}
