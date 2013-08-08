package de.stadtrallye.rallyesoft.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.rallye.model.structures.Task;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.executors.JSONArrayRequestExecutor;
import de.stadtrallye.rallyesoft.model.executors.RequestExecutor;
import de.stadtrallye.rallyesoft.model.converters.JsonConverters;

import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.strStr;

/**
 *
 */
public class Tasks implements ITasks, RequestExecutor.Callback<Tasks.CallbackIds> {

	private static final String THIS = Tasks.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);

	enum CallbackIds { TASKS_REFRESH }

	private final Model model;
	private List<ITasksListener> tasksListeners = new LinkedList<>();

	Tasks(Model model) {
		this.model = model;
	}


	@Override
	public void updateTasks() {
		if (!model.isConnectedInternal()) {
			err.notLoggedIn();
			return;
		}
		try {
			model.exec.execute(new JSONArrayRequestExecutor<>(model.factory.allTasksRequest(), new JsonConverters.TaskConverter(), this, CallbackIds.TASKS_REFRESH));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}

	public void updateTasksResult(JSONArrayRequestExecutor<Task, ?> r) {
		if (r.isSuccessful()) {
			try {
				List<Task> tasks = r.getResult();
				updateDatabase(tasks);
				notifyTaskUpdate();
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
		} else
			err.asyncTaskResponseError(r.getException());
	}

	private void updateDatabase(List<Task> tasks) {
		SQLiteDatabase db = model.db;

		db.beginTransaction();
		try {
			db.delete(DatabaseHelper.Tasks.TABLE, null, null);

			SQLiteStatement taskIn = db.compileStatement("INSERT INTO "+ DatabaseHelper.Tasks.TABLE +
					" ("+ strStr(DatabaseHelper.Tasks.COLS) +") VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			//KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_LOCATION_SPECIFIC, KEY_LAT, KEY_LON, KEY_MULTIPLE, KEY_SUBMIT_TYPE

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

				taskIn.bindLong(7, (t.multipleSubmits) ? 1 : 0);
				taskIn.bindLong(8, t.submitType);
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
				updateTasksResult((JSONArrayRequestExecutor<Task, ?>) r);
				break;
			default:
				Log.e(THIS, "Unknown Executor Callback");
				break;
		}
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
						DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC, DatabaseHelper.Tasks.KEY_LAT, DatabaseHelper.Tasks.KEY_LON, DatabaseHelper.Tasks.KEY_MULTIPLE, DatabaseHelper.Tasks.KEY_SUBMIT_TYPE},
				cond, null, null, null, DatabaseHelper.Tasks.KEY_LOCATION_SPECIFIC +" DESC");
		Log.i(THIS, "new Cursor: "+ c.getCount() +" rows");
		return c;
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
