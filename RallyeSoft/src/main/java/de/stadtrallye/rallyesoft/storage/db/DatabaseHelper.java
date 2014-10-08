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

package de.stadtrallye.rallyesoft.storage.db;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 32;
	private static final String DATABASE_NAME = "de.stadtrallye.rallyesoft.db";

	private int editedTables = 0;
	public static final int EDIT_TASKS = 1, EDIT_USERS = 2, EDIT_GROUPS = 4, EDIT_CHATROOMS = 8, EDIT_CHATS = 16, EDIT_NODES = 32, EDIT_EDGES = 64;

	public static final class Tasks {
		public static final String TABLE = "tasks";
		public static final String KEY_ID = "taskID";
		public static final String KEY_NAME = "name";
		public static final String KEY_DESCRIPTION = "description";
		public static final String KEY_LOCATION_SPECIFIC = "locationSpecific";
		public static final String KEY_LAT = "latitude";
		public static final String KEY_LON = "longitude";
		public static final String KEY_RADIUS = "radius";
		public static final String KEY_MULTIPLE = "multipleSubmits";
		public static final String KEY_SUBMIT_TYPE = "submitType";
		public static final String KEY_POINTS = "points";
		public static final String KEY_ADDITIONAL_RESOURCES = "additionalResources";
		public static final String KEY_SUBMITS = "submits";
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
						KEY_ID +" INTEGER PRIMARY KEY, "+
						KEY_NAME +" TEXT NOT NULL, "+
						KEY_DESCRIPTION +" TEXT NOT NULL, "+
						KEY_LOCATION_SPECIFIC + " INTEGER NOT NULL, "+
						KEY_LAT +" DOUBLE, "+
						KEY_LON +" DOUBLE, "+
						KEY_RADIUS +" DOUBLE NOT NULL, "+
						KEY_MULTIPLE +" INTEGER NOT NULL, "+
						KEY_SUBMIT_TYPE +" INTEGER NOT NULL, "+
						KEY_POINTS +" TEXT NOT NULL, "+
						KEY_ADDITIONAL_RESOURCES +" TEXT, "+
						KEY_SUBMITS +" INTEGER NOT NULL)";
		public static final String[] COLS = { KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_LOCATION_SPECIFIC, KEY_LAT, KEY_LON, KEY_RADIUS, KEY_MULTIPLE, KEY_SUBMIT_TYPE, KEY_POINTS, KEY_ADDITIONAL_RESOURCES, KEY_SUBMITS };
	}

	public static final class Submissions {
		public static final String TABLE = "submissions";
		public static final String KEY_ID = "submissionID";
		public static final String FOREIGN_TASK = Tasks.KEY_ID;
		public static final String KEY_TYPE = "submitType";
		public static final String KEY_PIC = "picSubmission";
		public static final String KEY_INT = "intSubmission";
		public static final String KEY_TEXT = "textSubmission";
		public static final String KEY_TIMESTAMP = "timestamp";
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
				KEY_ID +" INTEGER PRIMARY KEY, "+
				FOREIGN_TASK +" INTEGER NOT NULL, "+
				KEY_TYPE +" INTEGER NOT NULL, "+
				KEY_PIC +" TEXT, "+
				KEY_INT +" INTEGER, "+
				KEY_TEXT +" TEXT, "+
				KEY_TIMESTAMP +" TIMESTAMP NOT NULL)";
	}

	public static final class Users {
		public static final String TABLE = "users";
		public static final String KEY_ID = "userID";
		public static final String KEY_NAME = "userName";
		public static final String FOREIGN_GROUP = Groups.KEY_ID;
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
						KEY_ID +" INTEGER PRIMARY KEY, "+
						KEY_NAME +" TEXT NOT NULL, "+
						FOREIGN_GROUP +" INTEGER NOT NULL)";
		public static final String[] COLS = { KEY_ID, KEY_NAME };
	}
	
	public static final class Groups {
		public static final String TABLE = "groups";
		public static final String KEY_ID = "groupID";
		public static final String KEY_NAME = "groupName";
		public static final String KEY_DESCRIPTION = "description";
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
						KEY_ID +" INTEGER PRIMARY KEY, "+
						KEY_NAME +" TEXT NOT NULL, "+
						KEY_DESCRIPTION +" TEXT)";
		public static final String[] COLS = { KEY_ID, KEY_NAME, KEY_DESCRIPTION };
	}
	
	public static final class Chatrooms {
		public static final String TABLE = "chatrooms";
		public static final String KEY_ID = "chatroomID";
		public static final String KEY_NAME = "chatroomName";
		public static final String KEY_LAST_REFRESH = "lastRefresh";
		public static final String KEY_LAST_READ = "lastRead";
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
					KEY_ID +" INTEGER PRIMARY KEY, "+
					KEY_NAME +" TEXT NOT NULL, "+
					KEY_LAST_REFRESH +" TIMESTAMP NOT NULL, "+
					KEY_LAST_READ +" INTEGER NOT NULL)";
		public static final String[] COLS = { KEY_ID, KEY_NAME, KEY_LAST_REFRESH, KEY_LAST_READ};
	}
  
	public static final class Chats {
		public static final String TABLE = "chats";
		public static final String KEY_ID = "chatID";
		public static final String KEY_TIME = "timestamp";
		public static final String KEY_MESSAGE = "message";
		public static final String FOREIGN_USER = "userID";
		public static final String KEY_PICTURE = "pictureHash";
		public static final String FOREIGN_GROUP = Groups.KEY_ID;
		public static final String FOREIGN_ROOM = Chatrooms.KEY_ID;
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
				  KEY_ID +" INTEGER PRIMARY KEY, "+
				  KEY_TIME +" TIMESTAMP NOT NULL, "+
				  Groups.KEY_ID +" INTEGER NOT NULL, "+
				  FOREIGN_USER +" INTEGER NOT NULL, "+
				  KEY_MESSAGE +" TEXT, "+
				  KEY_PICTURE +" TEXT, "+
				  Chatrooms.KEY_ID +" INTEGER NOT NULL)";
		public static final String[] COLS = { KEY_ID, KEY_TIME, FOREIGN_GROUP, FOREIGN_USER, KEY_MESSAGE, KEY_PICTURE, FOREIGN_ROOM };
	}
    
    public static final class Nodes {
    	public static final String TABLE = "nodes";
    	public static final String KEY_ID = "nodeID";
    	public static final String KEY_NAME = "nodeName";
    	public static final String KEY_LAT = "latitude";
    	public static final String KEY_LON = "longitude";
    	public static final String KEY_DESCRIPTION = "description";
    	public static final String CREATE =
    			"CREATE TABLE "+ TABLE +" ("+
    					KEY_ID +" int PRIMARY KEY, "+
    					KEY_NAME +" TEXT NOT NULL, "+
    					KEY_LAT +" DOUBLE NOT NULL, "+
    					KEY_LON +" DOUBLE NOT NULL, "+
    					KEY_DESCRIPTION +" TEXT)";
    	public static final String[] COLS = { KEY_ID, KEY_NAME, KEY_LAT, KEY_LON, KEY_DESCRIPTION };
    }
    
    public static final class Edges {
    	public static final String TABLE = "edges";
    	public static final String KEY_A = "aNodeID";
    	public static final String KEY_B = "bNodeID";
    	public static final String KEY_TYPE = "type";
    	public static final String CREATE =
    			"CREATE TABLE "+ TABLE +" ("+
    					KEY_A +" INTEGER NOT NULL, "+
    					KEY_B +" INTEGER NOT NULL, "+
    					KEY_TYPE +" TEXT NOT NULL, "+
    					"PRIMARY KEY ("+ KEY_A+", "+ KEY_B +"))";
    	public static final String[] COLS = { KEY_A, KEY_B, KEY_TYPE };
    }

	public static final class Pictures {
		public static final String TABLE = "pictures";
		public static final String KEY_ID = "pID";
		public static final String KEY_HASH = "hash";
		public static final String KEY_FILE = "file";
		public static final String KEY_ADDED = "added";
		public static final String KEY_STATE = "state";
		public static final String KEY_SOURCE_HINT = "sourceHint";
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
						KEY_ID +" INTEGER PRIMARY KEY, "+
						KEY_HASH +" TEXT NULL, "+
						KEY_FILE +" TEXT NOT NULL, "+
						KEY_ADDED +" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, "+
						KEY_STATE +" INTEGER NOT NULL, "+
						KEY_SOURCE_HINT +" TEXT NULL)";
		public static final String[] COLS = {KEY_ID, KEY_HASH, KEY_FILE, KEY_ADDED, KEY_STATE, KEY_SOURCE_HINT};
	}

    public DatabaseHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Users.CREATE);
		db.execSQL(Groups.CREATE);
		db.execSQL(Chatrooms.CREATE);
		db.execSQL(Chats.CREATE);
		db.execSQL(Nodes.CREATE);
		db.execSQL(Edges.CREATE);
		db.execSQL(Tasks.CREATE);
		db.execSQL(Pictures.CREATE);
		db.execSQL(Submissions.CREATE);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w("Database", "Upgrading from version " + oldVersion + " to version " + newVersion);

		if (oldVersion <= 25) {
			drop(db, Chatrooms.TABLE);
			drop(db, Groups.TABLE);
			drop(db, Users.TABLE);
			drop(db, Chats.TABLE);
			drop(db, Nodes.TABLE);
			drop(db, Edges.TABLE);
			drop(db, Tasks.TABLE);
			this.onCreate(db);
			editedTables = EDIT_TASKS | EDIT_EDGES | EDIT_GROUPS | EDIT_NODES | EDIT_USERS | EDIT_CHATROOMS | EDIT_CHATS;
		} else if (oldVersion <= 26) {
			drop(db, Tasks.TABLE);
			db.execSQL(Tasks.CREATE);
			db.execSQL(Pictures.CREATE);
			drop(db, Chats.TABLE);
			db.execSQL(Chats.CREATE);
			editedTables = EDIT_TASKS | EDIT_CHATS;
			db.execSQL(Submissions.CREATE);
		} else if (oldVersion < 28) {
			db.execSQL(Pictures.CREATE);
			drop(db, Chats.TABLE);
			db.execSQL(Chats.CREATE);
			editedTables = EDIT_CHATS;
			db.execSQL(Submissions.CREATE);
		} else if (oldVersion < 29) {
			drop(db, Pictures.TABLE);
			db.execSQL(Pictures.CREATE);
			drop(db, Chats.TABLE);
			db.execSQL(Chats.CREATE);
			editedTables = EDIT_CHATS;
			db.execSQL(Submissions.CREATE);
		} else if (oldVersion < 30) {
			drop(db, Chats.TABLE);
			db.execSQL(Chats.CREATE);
			editedTables = EDIT_CHATS;
			drop(db, Pictures.TABLE);
			db.execSQL(Pictures.CREATE);
			db.execSQL(Submissions.CREATE);
		} else if (oldVersion < 31) {
			drop(db, Pictures.TABLE);
			db.execSQL(Pictures.CREATE);
			db.execSQL(Submissions.CREATE);
		} else if (oldVersion < 32) {
			db.execSQL(Submissions.CREATE);
		}
	}

	private void drop(SQLiteDatabase db, String table) {
		db.execSQL("DROP TABLE IF EXISTS "+ table);
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
	    if (!db.isReadOnly()) {
	        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
	        	enableForeignKeysApi16(db);
	        else
	        	enableForeignKeysApi8(db);
	    }
	}

	public int getEditedTables() {
		return editedTables;
	}

	/**
	 * Supported since Api 16
	 * @param db Database
	 */
	@TargetApi(16)
	private void enableForeignKeysApi16(SQLiteDatabase db) {
		db.setForeignKeyConstraintsEnabled(true);
	}
	
	/**
	 * Supported since SQLite shipped with Api 8
	 * @param db Database
	 */
	private void enableForeignKeysApi8(SQLiteDatabase db) {
		db.execSQL("PRAGMA foreign_keys=ON;");
	}
	
	public static String strStr(String... strings) {
		StringBuilder b = new StringBuilder();
		
		int l = strings.length-1;
		for (int i=0; i<=l; i++) {
			b.append(strings[i]);
			if (i < l)
				b.append(", ");
		}
		
		return b.toString();
	}

	public static boolean getBoolean(Cursor c, int row) {
		return (c.getInt(row) != 0);
	}

}