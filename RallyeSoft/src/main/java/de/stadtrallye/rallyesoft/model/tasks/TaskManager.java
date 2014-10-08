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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.List;

import de.rallye.model.structures.PushPrimarySubmissionConfig;
import de.rallye.model.structures.PushSubmission;
import de.rallye.model.structures.Submission;
import de.rallye.model.structures.TaskSubmissions;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.pictures.IPictureManager;
import de.stadtrallye.rallyesoft.net.retrofit.RetroAuthCommunicator;
import de.stadtrallye.rallyesoft.storage.IDbProvider;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.util.converters.CursorConverters;
import de.stadtrallye.rallyesoft.util.converters.Serialization;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_TASKS;
import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Submissions;
import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Tasks;
import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.strStr;

/**
 *
 */
public class TaskManager implements ITaskManager {

	private static final String THIS = TaskManager.class.getSimpleName();


	private final IDbProvider dbProvider;
	private final RetroAuthCommunicator comm;

	private final LruCache<Integer, de.stadtrallye.rallyesoft.model.tasks.Task> tasksCache = new LruCache<>(100); //Android manual says SoftReferences are inefficient because they have not enough information, recommends LruCache as max. efficient solution on Android

	private final List<ITasksListener> tasksListeners = new ArrayList<>();

	public TaskManager() throws NoServerKnownException {
		this(Server.getCurrentServer().getAuthCommunicator(), Storage.getDatabaseProvider());
	}

	public TaskManager(RetroAuthCommunicator communicator, IDbProvider dbProvider) {

		this.comm = communicator;
		this.dbProvider = dbProvider;

		if (dbProvider.hasStructureChanged(EDIT_TASKS)) {
			try {
				forceRefresh();
			} catch (NoServerKnownException e) {
				Log.w(THIS, "Purged Data, but could not refresh");
			}
			dbProvider.structureChangeHandled(EDIT_TASKS);
		}
	}

	private SQLiteDatabase getDb() {
		return dbProvider.getDatabase();
	}

	@Override
	public void forceRefresh() throws NoServerKnownException {
		getDb().delete(Tasks.TABLE, null, null);
		getDb().delete(Submissions.TABLE, null, null);

		update();
	}


	@Override
	public void update() throws NoServerKnownException {
		checkServerKnown();

		comm.getTasks(new Callback<List<de.rallye.model.structures.Task>>() {
			@Override
			public void success(List<de.rallye.model.structures.Task> tasks, Response response) {
				writeTasks(tasks);
				notifyTaskUpdate();
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Task Update failed", e);
				//TODO Server.getServer().commFailed(e);
			}
		});
		comm.getAllSubmissionsForGroup(new Callback<List<TaskSubmissions>>() {
			@Override
			public void success(List<TaskSubmissions> taskSubmissions, Response response) {
				writeSubmissions(taskSubmissions);
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Submissions Update failed", e);
				//TODO Server.getServer().commFailed(e);
			}
		});
	}

	private void writeSubmissions(List<TaskSubmissions> taskSubmissions) {
		ContentValues insert = new ContentValues();
		SQLiteDatabase db = getDb();

		for(TaskSubmissions taskSubmissions1 : taskSubmissions) {
			int taskID = taskSubmissions1.taskID;
			for (Submission submission : taskSubmissions1.submissions) {
				fillInsert(submission, insert, taskID);
				db.insertWithOnConflict(Submissions.TABLE, null, insert, SQLiteDatabase.CONFLICT_REPLACE);
			}
		}
	}

	@Override
	public ITask getTask(int taskID) {
		synchronized (tasksCache) {
			de.stadtrallye.rallyesoft.model.tasks.Task task = tasksCache.get(taskID);
			if (task == null) {
				Cursor c = getDb().query(Tasks.TABLE,
						new String[]{Tasks.KEY_ID + " AS _id", Tasks.KEY_NAME, Tasks.KEY_DESCRIPTION,
								Tasks.KEY_LOCATION_SPECIFIC, Tasks.KEY_LAT, Tasks.KEY_LON,
								Tasks.KEY_RADIUS, Tasks.KEY_MULTIPLE, Tasks.KEY_SUBMIT_TYPE,
								Tasks.KEY_POINTS, Tasks.KEY_ADDITIONAL_RESOURCES, Tasks.KEY_SUBMITS},
						Tasks.KEY_ID + "=" + taskID, null, null, null, Tasks.KEY_LOCATION_SPECIFIC + " DESC");
				c.moveToFirst();
				task = new de.stadtrallye.rallyesoft.model.tasks.Task(CursorConverters.getTask(c, tIds), this);
				tasksCache.put(taskID, task);
			}

			return task;
		}
	}

	/**
	 * Read the task at the current position, or from cache
	 * @param cursor
	 * @return
	 */
	@Override
	public ITask getTaskFromCursor(Cursor cursor) {
		synchronized (tasksCache) {
			int taskID = cursor.getInt(tIds.id);
			Task task = tasksCache.get(taskID);
			if (task == null) {
				task = new Task(CursorConverters.getTask(cursor, tIds), this);
				tasksCache.put(taskID, task);
			}

			return task;
		}
	}

	private static final CursorConverters.TaskCursorIds tIds = new CursorConverters.TaskCursorIds();
	static {
		tIds.id = 0;
		tIds.name = 1;
		tIds.description = 2;
		tIds.locationSpecific = 3;
		tIds.longitude = 5;
		tIds.latitude = 4;
		tIds.radius = 6;
		tIds.multiple = 7;
		tIds.submitType = 8;
		tIds.points = 9;
		tIds.additionalResources = 10;
		tIds.submits = 11;
	}

	private void writeTasks(List<de.rallye.model.structures.Task> tasks) {
		SQLiteDatabase db = getDb();

		db.beginTransaction();
		try {
			db.delete(Tasks.TABLE, null, null);

			SQLiteStatement taskIn = db.compileStatement("INSERT INTO "+ Tasks.TABLE +
					" ("+ strStr(Tasks.COLS) +") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			//KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_LOCATION_SPECIFIC, KEY_LAT, KEY_LON, KEY_RADIUS, KEY_MULTIPLE, KEY_SUBMIT_TYPE, KEY_POINTS, KEY_ADDITIONAL_RESOURCES, KEY_SUBMITS

			for (de.rallye.model.structures.Task t: tasks) {
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
				if (t.maxPoints != null) {
					taskIn.bindString(10, t.maxPoints);
				} else {
					taskIn.bindNull(10);
				}
				if (t.additionalResources == null || t.additionalResources.size() == 0) {
					taskIn.bindNull(11);
				} else {
					String add = Serialization.getJsonInstance().writeValueAsString(t.additionalResources);
					taskIn.bindString(11, add);
				}
//				if (submissions != null) {
//					List<Submission> subs = submissions.get(t.taskID);
//					taskIn.bindLong(12, Task.getSubmitsFromList(subs, t.multipleSubmits));
//				} else {
					taskIn.bindLong(12, -1);// The Value of Task.SUBMITS_UNKNOWN
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
//			db.update(DatabaseHelper.Tasks.TABLE, vals, DatabaseHelper.Tasks.KEY_ID +"="+ taskID, null);//TODO bad decision, store them in db
//		}
//	}

	void getSubmissionsFor(int taskID, Callback<List<Submission>> callback) {
		checkServerKnown();

		comm.getSubmissionsForTask(taskID, callback);
	}

	@Override
	public void submitSolution(final int taskID, int type, IPictureManager.IPicture picture, String text, Integer number, Location location) throws NoServerKnownException {
		checkServerKnown();

		GeoPostSubmission post = new GeoPostSubmission(type, (picture != null)? picture.getHash() : null, number, text, location);
		comm.postSubmission(taskID, post, new Callback<Submission>() {
			@Override
			public void success(Submission submission, Response response) {
				writeSubmission(submission, taskID);

				de.stadtrallye.rallyesoft.model.tasks.Task task = tasksCache.get(taskID);
				if (task != null)
					task.addSubmission(submission);
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
		synchronized (tasksListeners) {
			tasksListeners.add(l);
		}
	}

	@Override
	public void removeListener(ITasksListener l) {
		synchronized (tasksListeners) {
			tasksListeners.remove(l);
		}
	}

	private void notifyTaskUpdate() {
		Handler handler;
		synchronized (tasksListeners) {
			for (final ITasksListener l : tasksListeners) {
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
		String cond = (locationSpecific != null)? Tasks.KEY_LOCATION_SPECIFIC+"="+ ((locationSpecific)? 1 : 0) : null;

		Cursor c = getDb().query(Tasks.TABLE,
				new String[]{Tasks.KEY_ID + " AS _id", Tasks.KEY_NAME, Tasks.KEY_DESCRIPTION,
						Tasks.KEY_LOCATION_SPECIFIC, Tasks.KEY_LAT, Tasks.KEY_LON,
						Tasks.KEY_RADIUS, Tasks.KEY_MULTIPLE, Tasks.KEY_SUBMIT_TYPE,
						Tasks.KEY_POINTS, Tasks.KEY_ADDITIONAL_RESOURCES, Tasks.KEY_SUBMITS},
				cond, null, null, null, Tasks.KEY_LOCATION_SPECIFIC + " DESC");
		Log.i(THIS, "new Cursor: "+ c.getCount() +" rows");
		return c;
	}

	@Override
	public int getLocationSpecificTasksCount() {
		Cursor c = getDb().query(Tasks.TABLE, new String[]{"count(*)"}, Tasks.KEY_LOCATION_SPECIFIC + "=" + 1, null, null, null, null);
		c.moveToFirst();
		int res = c.getInt(0);
		c.close();

		return res;
	}

	@Override
	public void pushSubmission(PushSubmission push) {
		Submission submission = push.submission;

		writeSubmission(submission, push.taskID);

		Task task = tasksCache.get(push.taskID);
		if (task != null) {
			task.addSubmission(submission);
		}
	}

	private void writeSubmission(Submission submission, int taskID) {
		ContentValues insert = new ContentValues();

		fillInsert(submission, insert, taskID);

		getDb().insertWithOnConflict(Submissions.TABLE, null, insert, SQLiteDatabase.CONFLICT_REPLACE);
	}

	private void fillInsert(Submission submission, ContentValues insert, int taskID) {
		insert.put(Submissions.KEY_ID, submission.submissionID);
		insert.put(Submissions.FOREIGN_TASK, taskID);
		insert.put(Submissions.KEY_TYPE, submission.submitType);
		insert.put(Submissions.KEY_PIC, submission.picSubmission);
		insert.put(Submissions.KEY_INT, submission.intSubmission);
		insert.put(Submissions.KEY_TEXT, submission.textSubmission);
		insert.put(Submissions.KEY_TIMESTAMP, submission.timestamp);
	}

	@Override
	public void pushActiveSubmission(PushPrimarySubmissionConfig primary) {
//		getDb().update()//TODO

		Task task = tasksCache.get(primary.taskID);
		if (task != null) {
			task.changePrimarySubmission(primary.primarySubmissionID);
		}
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

	public List<Submission> readSubmissions(int taskID) {
		Cursor c = getDb().query(Submissions.TABLE, new String[]{Submissions.KEY_ID, Submissions.KEY_TYPE, Submissions.KEY_PIC, Submissions.KEY_INT, Submissions.KEY_TEXT, Submissions.KEY_TIMESTAMP}, Submissions.FOREIGN_TASK +"=?", new String[]{Integer.toString(taskID)}, null, null, Submissions.KEY_TIMESTAMP+" DESC");

		List<Submission> submissions = new ArrayList<>();
		while (c.moveToNext()) {
			Submission submission = new Submission(c.getInt(0), c.getInt(1), c.getString(2), c.getInt(3), c.getString(4), c.getLong(5));
			submissions.add(submission);
		}

		c.close();

		return submissions;
	}
}
