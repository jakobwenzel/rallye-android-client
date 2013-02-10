package de.stadtrallye.rallyesoft.model.backend;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 9;
	private static final String DATABASE_NAME = "de.stadtrallye.rallyesoft.db";
	
	public static final class Groups {
		public static final String TABLE = "groups";
		public static final String KEY_ID = "groupID";
		public static final String KEY_NAME = "name";
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
						KEY_ID +" INTEGER PRIMARY KEY, "+
						KEY_NAME +" VARCHAR(50))";
	}
	
	public static final class Chatrooms {
		public static final String TABLE = "chatrooms";
		public static final String KEY_ID = "chatroomID";
		public static final String KEY_NAME = "name";
		public static final String KEY_LAST_UPDATE = "lastUpdate";
		public static final String KEY_LAST_ID = "lastID";
		public static final String FOREIGN_GROUP = Groups.KEY_ID;
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
					KEY_ID +" INTEGER PRIMARY KEY, "+
					KEY_NAME +" VARCHAR(50) NOT NULL, "+
					Groups.KEY_ID +" INTEGER NOT NULL REFERENCES "+ Groups.TABLE +" ON DELETE CASCADE ON UPDATE CASCADE, "+
					KEY_LAST_UPDATE +" timestamp, "+
					KEY_LAST_ID +" INTEGER)";
	}
  
	public static final class Chats {
		public static final String TABLE = "chats";
		public static final String KEY_ID = "chatID";
		public static final String KEY_TIME = "timestamp";
		public static final String KEY_SELF = "self";
		public static final String KEY_PICTURE = "pictureID";
		public static final String FOREIGN_GROUP = Groups.KEY_ID;
		public static final String FOREIGN_MSG = Messages.KEY_ID;
		public static final String FOREIGN_ROOM = Chatrooms.KEY_ID;
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
				  KEY_ID +" INTEGER PRIMARY KEY, "+
				  KEY_TIME +" timestamp NOT NULL, "+
				  Groups.KEY_ID +" int NOT NULL REFERENCES "+ Groups.TABLE +" ON DELETE CASCADE ON UPDATE CASCADE, "+
				  KEY_SELF +" BOOLEAN NOT NULL DEFAULT false, "+
				  Messages.KEY_ID +" int DEFAULT NULL REFERENCES "+ Messages.TABLE +" ON DELETE CASCADE ON UPDATE CASCADE, "+
				  KEY_PICTURE +" int DEAULT NULL, "+
				  Chatrooms.KEY_ID +" int NOT NULL REFERENCES "+ Chatrooms.TABLE +" ON DELETE CASCADE ON UPDATE CASCADE)";
	}
	
    public static final class Messages {
    	public static final String TABLE = "messages";
		public static final String KEY_ID = "msgID";
		public static final String KEY_MSG = "msg";
    	public static final String CREATE =
                "CREATE TABLE "+ TABLE +" ("+
                KEY_ID +" INTEGER PRIMARY KEY, "+
                KEY_MSG + " TEXT)";
    }
    
//    private static final class Pictures {
//    	public static final String NAME = "pictures";
//    	public static final String KEY_ID = "pictureID";
//    	public static final String CREATE =
//    			"CREATE TABLE "+ NAME +" ("+
//    					KEY_ID +" int AUTOINCREMENT PRIMARY KEY,"+
//    					")";
//    }

    public DatabaseOpenHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Groups.CREATE);
		db.execSQL(Chatrooms.CREATE);
		db.execSQL(Messages.CREATE);
		db.execSQL(Chats.CREATE);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE IF EXISTS "+ Groups.TABLE);
		db.execSQL("DROP TABLE IF EXISTS "+ Chatrooms.TABLE);
		db.execSQL("DROP TABLE IF EXISTS "+ Chats.TABLE);
		db.execSQL("DROP TABLE IF EXISTS "+ Messages.TABLE);
		
		this.onCreate(db);
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
	
	/**
	 * Supported since Api 16
	 * @param db
	 */
	@TargetApi(16)
	private void enableForeignKeysApi16(SQLiteDatabase db) {
		db.setForeignKeyConstraintsEnabled(true);
	}
	
	/**
	 * Supported since SQLite shipped with Api 8
	 * @param db
	 */
	private void enableForeignKeysApi8(SQLiteDatabase db) {
		db.execSQL("PRAGMA foreign_keys=ON;");
	}

}