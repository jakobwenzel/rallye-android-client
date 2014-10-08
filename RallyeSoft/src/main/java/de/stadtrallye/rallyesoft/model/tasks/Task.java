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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.LatLng;
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
	private final ReadWriteLock submissionsLock = new ReentrantReadWriteLock();
	private boolean refreshingSubs;

	public Task(de.rallye.model.structures.Task task, TaskManager manager) {
		this.task = task;
		this.manager = manager;
	}


	@Override
	public LatLng getLocation() {
		return task.location;
	}

	@Override
	public void addListener(ITaskListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeListener(ITaskListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	private void notifySubmissionsChanged() {
		Handler handler;
		synchronized (listeners) {
			submissionsLock.readLock().lock();
			try {
				for (final ITaskListener listener : listeners) {
					handler = listener.getCallbackHandler();
					if (handler != null) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								submissionsLock.readLock().lock();
								try {
									listener.onSubmissionsChanged(submissions);
								} finally {
									submissionsLock.readLock().unlock();
								}
							}
						});
					} else {
						listener.onSubmissionsChanged(submissions);
					}
				}
			} finally {
				submissionsLock.readLock().unlock();
			}
		}
	}

	@Override
	public void updateSubmissions() {
		synchronized (this) {
			if (refreshingSubs) {
				Log.w(THIS, "Preventing concurrent Submission update");
				return;
			}
			refreshingSubs = true;
		}

		manager.getSubmissionsFor(task.taskID, new Callback<List<Submission>>() {
			@Override
			public void success(List<Submission> submissions, Response response) {
				submissionsLock.writeLock().lock();
				try {
					Task.this.submissions = submissions;
				} finally {
					submissionsLock.writeLock().unlock();
				}
				notifySubmissionsChanged();

				resetRefreshingSubs();
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Submit failed", e);
				//TODO Server.getServer().commFailed(e);
				resetRefreshingSubs();
			}
		});
	}

	private void resetRefreshingSubs() {
		synchronized (this) {
			refreshingSubs = false;
		}
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
	public List<Submission> getSubmissionsCached() {
		submissionsLock.readLock().lock();
		try {
			return submissions;
		} finally {
			submissionsLock.readLock().unlock();
		}
	}

	@Override
	public boolean hasLocation() {
		return task.hasLocation();
	}

	@Override
	public double getRadius() {
		return task.radius;
	}

	void addSubmission(Submission submission) {
		submissionsLock.writeLock().lock();
		try {
			if (submissions == null) {
				submissions = new ArrayList<>();
			}
			submissions.add(submission);
		} finally {
			submissionsLock.writeLock().unlock();
		}
		notifySubmissionsChanged();
	}
}
