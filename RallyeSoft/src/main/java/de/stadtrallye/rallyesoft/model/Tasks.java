package de.stadtrallye.rallyesoft.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.Submission;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayToMapRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.JSONObjectRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.model.converters.JsonConverters;
import de.stadtrallye.rallyesoft.model.structures.Task;
import de.stadtrallye.rallyesoft.uimodel.IPictureTakenListener;

import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.EDIT_TASKS;
import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.getBoolean;
import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.strStr;

/**
 *
 */
public class Tasks implements ITasks, RequestExecutor.Callback<Tasks.CallbackIds> {

	private static final String THIS = Tasks.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);

	enum CallbackIds {SUBMISSIONS_REFRESH, TASKS_REFRESH }

	private final Model model;

	private Map<Integer, List<Submission>> submissions;

	private final List<ITasksListener> tasksListeners = new ArrayList<ITasksListener>();

	Tasks(Model model) {
		this.model = model;

		if ((model.deprecatedTables & EDIT_TASKS) > 0) {
			refresh();
			model.deprecatedTables &= ~EDIT_TASKS;
		}
	}


	@Override
	public void refresh() {
		if (!model.isConnectedInternal()) {
			err.notLoggedIn();
			return;
		}
		try {
			model.exec.execute(new JSONArrayRequestExecutor<Task, CallbackIds>(model.factory.allTasksRequest(), new JsonConverters.TaskConverter(), this, CallbackIds.TASKS_REFRESH));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}

	private void refreshResult(JSONArrayRequestExecutor<Task, ?> r) {
		if (r.isSuccessful()) {
			try {
				List<Task> tasks = r.getResult();
				updateDatabase(tasks);
				notifyTaskUpdate();
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
		} else {
			err.asyncTaskResponseError(r.getException());
			model.commError(r.getException());
		}
	}

	private void updateDatabase(List<Task> tasks) {
		SQLiteDatabase db = model.db;

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
				if (submissions != null) {
					List<Submission> subs = submissions.get(t.taskID);
					taskIn.bindLong(12, Task.getSubmitsFromList(subs, t.multipleSubmits));
				} else {
					taskIn.bindLong(12, Task.SUBMITS_UNKNOWN);
				}
				taskIn.executeInsert();
			}

			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e(THIS, "Tasks Update on Database failed", e);
		} finally {
			db.endTransaction();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void executorResult(RequestExecutor<?, CallbackIds> r, CallbackIds callbackId) {
		switch (callbackId) {
			case TASKS_REFRESH:
				refreshResult((JSONArrayRequestExecutor<Task, ?>) r);
				break;
			case SUBMISSIONS_REFRESH:
				refreshSubmissionsResult((RequestExecutor<Map<Integer, List<Submission>>, ?>) r);
				break;
			default:
				Log.e(THIS, "Unknown Executor Callback");
				break;
		}
	}

	@Override
	public void refreshSubmissions() {
		if (!model.isConnectedInternal()) {
			err.notLoggedIn();
			return;
		}
		try {
			model.exec.execute(new JSONArrayToMapRequestExecutor<Integer, de.rallye.model.structures.TaskSubmissions, List<Submission>, CallbackIds>(model.factory.allSubmissionsRequest(model.getUser().groupID),
					new JsonConverters.TaskSubmissionsConverter(),
					new JsonConverters.TaskSubmissionsIndexer(),
					new JsonConverters.TaskSubmissionsCompressor(), this, CallbackIds.SUBMISSIONS_REFRESH));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}

	private void refreshSubmissionsResult(RequestExecutor<java.util.Map<Integer, List<Submission>>, ?> r) {
		if (r.isSuccessful()) {
			try {
				java.util.Map<Integer, List<Submission>> subs = r.getResult();
				saveSubmissions(subs);
				notifyTaskUpdate();
				notifySubmissions(subs);
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
		} else {
			err.asyncTaskResponseError(r.getException());
			model.commError(r.getException());
		}
	}

	private void saveSubmissions(Map<Integer, List<Submission>> submissions) {
		this.submissions = submissions;

		Cursor c = getTasksCursor();

		for (int i=0; i<c.getCount(); i++) {
			c.moveToNext();
			int taskID = c.getInt(0);
			int submits = Task.getSubmitsFromList(submissions.get(taskID), getBoolean(c, 7));
			ContentValues vals = new ContentValues();
			vals.put(DatabaseHelper.Tasks.KEY_SUBMITS, submits);
			model.db.update(DatabaseHelper.Tasks.TABLE, vals, DatabaseHelper.Tasks.KEY_ID +"="+ taskID, null);
		}
	}

	@Override
	public void submitSolution(int taskID, int type, IPictureTakenListener.Picture picture, String text, String number) {
		if (!model.isConnected()) {
			err.notLoggedIn();
			return;
		}
		try {
			model.exec.execute(new JSONObjectRequestExecutor<>(model.factory., this, CallbackIds.SUBMIT_SOLUTION));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}

	@Override
	public Map<Integer, List<Submission>> getSubmissions() {
		return submissions;
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
		model.uiHandler.post(new Runnable(){
			@Override
			public void run() {
				for(ITasksListener l: tasksListeners) {
					l.taskUpdate();
				}
			}
		});
	}

	private void notifySubmissions(final java.util.Map<Integer, List<Submission>> submissions) {
		model.uiHandler.post(new Runnable(){
			@Override
			public void run() {
				for(ITasksListener l: tasksListeners) {
					l.submissionsUpdate(submissions);
				}
			}
		});
	}

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

		Cursor c = model.db.query(DatabaseHelper.Tasks.TABLE,
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
		Cursor c = model.db.query(DatabaseHelper.Tasks.TABLE, new String[]{"count(*)"}, DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC +"="+1, null, null, null, null);
		c.moveToFirst();
		int res = c.getInt(0);
		c.close();

		return res;
	}

	@Override
	public int getTaskPositionInCursor(int initialTaskId) {
		if (initialTaskId < 0)
			return 0;

		Cursor c = getTasksCursor(null);

		int id,i = 0;
		while (c.moveToNext()) {
			id = c.getInt(0);

			if (id == initialTaskId) {
				return i;
			}
			i++;
		}
		c.close();
		return 0;
	}
}
