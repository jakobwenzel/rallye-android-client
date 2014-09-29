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

package de.stadtrallye.rallyesoft.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper;

/**
 * Created by Ramon on 22.09.2014.
 */
public class Storage {

	private static final String THIS = Storage.class.getSimpleName();

	private static final String SERVER_CONFIG = "server_config.json";
	private static final String MAP_CONFIG = "map_config.json";

	private static List<Object> handles = new ArrayList<>();

	private static Context context;
	private static final DatabaseProvider dbProvider = new DatabaseProvider();

	public static synchronized void aquireStorage(Context applicationContext, Object handle) {
		if (handles.isEmpty()) {
			Storage.context = applicationContext;
			dbProvider.setContext(applicationContext);
			Log.d(THIS, "Creating new Storage context");
		}

		handles.add(handle);
	}

	public static synchronized void releaseStorage(Object handle) {
		handles.remove(handle);

		if (handles.isEmpty()) {
			dbProvider.close();
			Log.d(THIS, "Last handle on Storage released");
		}
	}


	public static FileOutputStream getServerConfigOutputStream() throws FileNotFoundException {
		return context.openFileOutput(SERVER_CONFIG, Context.MODE_PRIVATE);
	}

	public static FileInputStream getServerConfigInputStream() throws FileNotFoundException {
		return context.openFileInput(SERVER_CONFIG);
	}

	public static FileOutputStream getMapConfigOutputStream() throws FileNotFoundException {
		return context.openFileOutput(MAP_CONFIG, Context.MODE_PRIVATE);
	}

	public static FileInputStream getMapConfigInputStream() throws FileNotFoundException {
		return context.openFileInput(MAP_CONFIG);
	}

	public static IDbProvider getDatabaseProvider() {
		return dbProvider;
	}

	private static class DatabaseProvider implements IDbProvider {
		private DatabaseHelper helper;
		private int changedTables;
		private SQLiteDatabase db;

		public void close() {
			helper.close();
			helper = null;
			db = null;
			changedTables = 0;
			Log.d(THIS, "Closed Database");
		}

		@Override
		public boolean hasStructureChanged(int table) {
			return (changedTables & table) > 0;
		}

		@Override
		public void structureChangeHandled(int table) {
			changedTables &= ~table;
		}

		@Override
		public SQLiteDatabase getDatabase() throws RuntimeException {
			if (db == null) {
				db = helper.getWritableDatabase();
				Log.d(THIS, "Opened a new Database");
			}
			return db;
		}

		public void setContext(Context applicationContext) {
			db = null;
			helper = new DatabaseHelper(applicationContext);
			changedTables = helper.getEditedTables();
		}
	}
}