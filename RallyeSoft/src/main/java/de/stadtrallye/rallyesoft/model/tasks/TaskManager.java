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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.SimpleSubmission;
import de.rallye.model.structures.SimpleSubmissionWithPictureHash;
import de.rallye.model.structures.Submission;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.structures.Task;
import de.stadtrallye.rallyesoft.net.Server;
import de.stadtrallye.rallyesoft.net.retrofit.RetroAuthCommunicator;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.uimodel.IPicture;
import de.stadtrallye.rallyesoft.util.converters.CursorConverters;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_TASKS;
import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.strStr;

/**
 *
 */
public class TaskManager implements ITaskManager {

	private static final String THIS = TaskManager.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);


	private final SQLiteDatabase db;
	private final RetroAuthCommunicator comm;
	private final int groupID;

	private Map<Integer, de.stadtrallye.rallyesoft.model.tasks.Task> tasksCache = new WeakHashMap<>();

	private final List<ITasksListener> tasksListeners = new ArrayList<>();

	public TaskManager() throws NoServerKnownException {
		this(Server.getCurrentServer().getAuthCommunicator(), Storage.getDatabase());
	}

	public TaskManager(RetroAuthCommunicator communicator, SQLiteDatabase db) {

		this.comm = communicator;
		this.groupID = Server.getCurrentServer().getGroupID();
		this.db = db;

		if (Storage.hasStructureChanged(EDIT_TASKS)) {
			try {
				forceRefresh();
			} catch (NoServerKnownException e) {
				Log.w(THIS, "Purged Data, but could not refresh");
			}
			Storage.structureChangeHandled(EDIT_TASKS);
		}
	}

	@Override
	public void forceRefresh() throws NoServerKnownException {
		db.delete(DatabaseHelper.Tasks.TABLE, null, null);

		update();
	}


	@Override
	public void update() throws NoServerKnownException {
		checkServerKnown();

		comm.getTasks(new Callback<List<Task>>() {
			@Override
			public void success(List<Task> tasks, Response response) {
				updateDatabase(tasks);
				notifyTaskUpdate();
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Task Update failed", e);
				//TODO Server.getServer().commFailed(e);
			}
		});
	}

	@Override
	public ITask getTask(int taskID) {
		de.stadtrallye.rallyesoft.model.tasks.Task task = tasksCache.get(taskID);
		if (task == null) {
			Cursor c = db.query(DatabaseHelper.Tasks.TABLE,
					new String[]{DatabaseHelper.Tasks.KEY_ID + " AS _id", DatabaseHelper.Tasks.KEY_NAME, DatabaseHelper.Tasks.KEY_DESCRIPTION,
							DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC, DatabaseHelper.Tasks.KEY_LAT, DatabaseHelper.Tasks.KEY_LON,
							DatabaseHelper.Tasks.KEY_RADIUS, DatabaseHelper.Tasks.KEY_MULTIPLE, DatabaseHelper.Tasks.KEY_SUBMIT_TYPE,
							DatabaseHelper.Tasks.KEY_POINTS, DatabaseHelper.Tasks.KEY_ADDITIONAL_RESOURCES, DatabaseHelper.Tasks.KEY_SUBMITS},
					DatabaseHelper.Tasks.KEY_ID + "=" + taskID, null, null, null, DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC + " DESC");
			c.moveToFirst();
			task = new de.stadtrallye.rallyesoft.model.tasks.Task(CursorConverters.getTask(c, CursorConverters.TaskCursorIds.read(c)), this);//TODO do not do TaskCursorIds.read(), statically assign
		}

		tasksCache.put(taskID, task);

		return task;
	}

	private void updateDatabase(List<Task> tasks) {

		db.beginTransaction();
		try {
			db.delete(DatabaseHelper.Tasks.TABLE, null, null);

			SQLiteStatement taskIn = db.compileStatement("INSERT INTO "+ DatabaseHelper.Tasks.TABLE +
					" ("+ strStr(DatabaseHelper.Tasks.COLS) +") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			//KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_LOCATION_SPECIFIC, KEY_LAT, KEY_LON, KEY_RADIUS, KEY_MULTIPLE, KEY_SUBMIT_TYPE, KEY_POINTS, KEY_ADDITIONAL_RESOURCES, KEY_SUBMITS

			for (Task t: tasks) {
				taskIn.bindLong(1, t.taskID);
				taskIn.bindString(2, t.name);
				taskIn.bindString(3, t.description);
				taskIn.bindLong(4, (t.locationSpecific)? 1 : 0);
				if (t.location == null) {
					taskIn.bindNull(5);
					taskIn.bindNull(6);
				} else {
					taskIn.bindDouble(5, t.location.latitude);
					taskIn.bindDouble(6, t.location.longitude);
				}

				taskIn.bindDouble(7, t.radius);
				taskIn.bindLong(8, (t.multipleSubmits) ? 1 : 0);
				taskIn.bindLong(9, t.submitType);
				taskIn.bindString(10, t.maxPoints);
				String add = AdditionalResource.additionalResourcesToString(t.additionalResources);
				if (add == null) {
					taskIn.bindNull(11);
				} else {
					taskIn.bindString(11, add);
				}
//				if (submissions != null) {
//					List<Submission> subs = submissions.get(t.taskID);
//					taskIn.bindLong(12, Task.getSubmitsFromList(subs, t.multipleSubmits));
//				} else {
					taskIn.bindLong(12, Task.SUBMITS_UNKNOWN);
//				}
				taskIn.executeInsert();
			}

			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e(THIS, "Tasks Update on Database failed", e);
		} finally {
			db.endTransaction();
		}
	}


//	@Override
//	public void refreshSubmissions() {
//		checkKnownServer();
//
//		comm.getAllSubmissionsForGroup(groupID, new Callback<List<Submission>>() {
//			@Override
//			public void success(List<Submission> submissions, Response response) {
//				saveSubmissions(submissions);
//				notifyTaskUpdate();
//				notifySubmissions(submissions);
//			}
//
//			@Override
//			public void failure(RetrofitError e) {
//				Log.e(THIS, "Submission Update failed", e);
//				//TODO Server.getServer().commFailed(e);
//			}
//		});
//	}

//	private void saveSubmissions(Map<Integer, List<Submission>> submissions) {
//		this.submissions = submissions;
//
//		Cursor c = getTasksCursor();
//
//		for (int i=0; i<c.getCount(); i++) {
//			c.moveToNext();
//			int taskID = c.getInt(0);
//			int submits = Task.getSubmitsFromList(submissions.get(taskID), getBoolean(c, 7));
//			ContentValues vals = new ContentValues();
//			vals.put(DatabaseHelper.Tasks.KEY_SUBMITS, submits);
//			db.update(DatabaseHelper.Tasks.TABLE, vals, DatabaseHelper.Tasks.KEY_ID +"="+ taskID, null);//TODO bad decision, do sth else
//		}
//	}

	void getSubmissionsFor(int taskID, Callback<List<Submission>> callback) {
		checkServerKnown();

		comm.getSubmissionsForTask(taskID, callback);
	}

	@Override
	public void submitSolution(int taskID, int type, IPicture picture, String text, Integer number) throws NoServerKnownException {
		checkServerKnown();

		SimpleSubmission post = (picture==null)? new SimpleSubmission(type, number, text) : new SimpleSubmissionWithPictureHash(type, text, picture.getHash());
		comm.postSubmission(taskID, post, new Callback<Submission>() {
			@Override
			public void success(Submission submission, Response response) {
				//TODO
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Submit failed", e);
				//TODO Server.getServer().commFailed(e);
			}
		});
	}

	@Override
	public void addListener(ITasksListener l) {
		tasksListeners.add(l);
	}

	@Override
	public void removeListener(ITasksListener l) {
		tasksListeners.remove(l);
	}

	private void notifyTaskUpdate() {
		Handler handler;
		for (final ITasksListener l: tasksListeners) {
			handler = l.getCallbackHandler();
			if (handler == null) {
				l.taskUpdate();
			} else {
				handler.post(new Runnable() {
					@Override
					public void run() {
						l.taskUpdate();
					}
				});
			}
		}
	}

//	private void notifySubmissions(final java.util.Map<Integer, List<Submission>> submissions) {
//		for(ITasksListener l: tasksListeners) {
//			l.submissionsUpdate(submissions);
//		}
//	}

//	@Override
//	public Cursor getLocationSpecificTasksCursor() {
//		return getTasksCursor(true);
//	}
//
//	@Override
//	public Cursor getUbiquitousTasksCursor() {
//		return getTasksCursor(false);
//	}

	@Override
	public Cursor getTasksCursor() {
		return getTasksCursor(null);
	}

	private Cursor getTasksCursor(Boolean locationSpecific) {
		String cond = (locationSpecific != null)? DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC+"="+ ((locationSpecific)? 1 : 0) : null;

		Cursor c = db.query(DatabaseHelper.Tasks.TABLE,
				new String[]{DatabaseHelper.Tasks.KEY_ID + " AS _id", DatabaseHelper.Tasks.KEY_NAME, DatabaseHelper.Tasks.KEY_DESCRIPTION,
						DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC, DatabaseHelper.Tasks.KEY_LAT, DatabaseHelper.Tasks.KEY_LON,
						DatabaseHelper.Tasks.KEY_RADIUS, DatabaseHelper.Tasks.KEY_MULTIPLE, DatabaseHelper.Tasks.KEY_SUBMIT_TYPE,
						DatabaseHelper.Tasks.KEY_POINTS, DatabaseHelper.Tasks.KEY_ADDITIONAL_RESOURCES, DatabaseHelper.Tasks.KEY_SUBMITS},
				cond, null, null, null, DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC +" DESC");
		Log.i(THIS, "new Cursor: "+ c.getCount() +" rows");
		return c;
	}

	@Override
	public int getLocationSpecificTasksCount() {
		Cursor c = db.query(DatabaseHelper.Tasks.TABLE, new String[]{"count(*)"}, DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC +"="+1, null, null, null, null);
		c.moveToFirst();
		int res = c.getInt(0);
		c.close();

		return res;
	}

	public static int findTaskPositionInCursor(int taskID, Cursor cursor, CursorConverters.TaskCursorIds c) {
		if (taskID < 0)
			return 0;

		int currentID;
		int pos = 0;
		cursor.moveToFirst();
		while (cursor.moveToNext()) {
			currentID = cursor.getInt(c.id);

			if (currentID == taskID) {
				return pos;
			}
			pos++;
		}

		return 0;
	}

	private void checkServerKnown() throws NoServerKnownException {
		if (comm == null)
			throw new NoServerKnownException();
	}
}
