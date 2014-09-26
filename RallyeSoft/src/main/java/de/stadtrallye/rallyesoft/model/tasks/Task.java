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

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.Submission;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Ramon on 23.09.2014.
 */
public class Task implements ITask {

	private static final String THIS = Task.class.getSimpleName();

	private final TaskManager manager;
	private final de.rallye.model.structures.Task task;
	private final List<ITaskListener> listeners = new ArrayList<>();
	private List<Submission> submissions;

	public Task(de.rallye.model.structures.Task task, TaskManager manager) {
		this.task = task;
		this.manager = manager;
	}


	@Override
	public void addListener(ITaskListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ITaskListener listener) {
		listeners.remove(listener);
	}

	private void notifySubmissionsChanged() {
		Handler handler;
		for (final ITaskListener listener: listeners) {
			handler = listener.getCallbackHandler();
			if (handler != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						listener.onSubmissionsChanged(submissions);
					}
				});
			} else {
				listener.onSubmissionsChanged(submissions);
			}
		}
	}

	@Override
	public void requestSubmissions() {
		manager.getSubmissionsFor(task.taskID, new Callback<List<Submission>>() {
			@Override
			public void success(List<Submission> submissions, Response response) {
				Task.this.submissions = submissions;
				notifySubmissionsChanged();
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Submit failed", e);
				//TODO Server.getServer().commFailed(e);
			}
		});
	}

	@Override
	public int getTaskID() {
		return task.taskID;
	}

	@Override
	public String getName() {
		return task.name;
	}

	@Override
	public String getDescription() {
		return task.description;
	}

	@Override
	public List<AdditionalResource> getAdditionalResources() {
		return task.additionalResources;
	}

	@Override
	public int getSubmitType() {
		return task.submitType;
	}

	@Override
	public boolean hasSubmissionsCached() {
		return submissions != null;
	}

	@Override
	public List<Submission> getSubmissionsCached() {
		return submissions;
	}
}
