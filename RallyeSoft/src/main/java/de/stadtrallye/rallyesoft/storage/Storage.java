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

	private static final String SERVER_CONFIG = "server_config.json";
	private static final String MAP_CONFIG = "map_config.json";

	private static List<Object> handles = new ArrayList<>();

	private static DatabaseHelper database;
	private static int changedTables;
	private static Context context;

	public static synchronized void aquireStorage(Context applicationContext, Object handle) {
		if (handles.isEmpty()) {
			Storage.context = applicationContext;
			database = new DatabaseHelper(applicationContext);
			changedTables = database.getEditedTables();
		}

		handles.add(handle);
	}

	public static synchronized void releaseStorage(Object handle) {
		handles.remove(handle);

		if (handles.isEmpty()) {
			database.close();
		}
	}

	public static SQLiteDatabase getDatabase() {
		return database.getWritableDatabase();
	}

	public static boolean hasStructureChanged(int table) {
		return (changedTables & table) > 0;
	}

	public static void structureChangeHandled(int table) {
		changedTables &= ~table;
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
}
