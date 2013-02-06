package de.stadtrallye.rallyesoft.model.backend;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 4;
	private static final String DATABASE_NAME = "de.stadtrallye.rallyesoft.db";
	
	private static final class Chatrooms {
		public static final String NAME = "chatrooms";
		public static final String KEY_ID = "chatroomID";
		public static final String KEY_NAME = "name";
		public static final String KEY_LAST_UPDATE = "lastUpdate";
		public static final String KEY_LAST_READ = "lastRead";
		public static final String CREATE =
				"CREATE TABLE "+ NAME +" ("+
					KEY_ID +" int NOT NULL PRIMARY KEY, "+
					KEY_NAME +" VARCHAR(50) NOT NULL, "+
					KEY_LAST_UPDATE +" timestamp, "+
					KEY_LAST_READ +" int)";
	}
  
	private static final class Chats {
		public static final String NAME = "chats";
		public static final String KEY_ID = "chatID";
		public static final String KEY_TIME = "timestamp";
		public static final String KEY_GROUP = "groupID";
		public static final String KEY_SELF = "self";
		public static final String KEY_PICTURE = "pictureID";
		public static final String CREATE =
				"CREATE TABLE "+ NAME +" ("+
				  KEY_ID +" int NOT NULL PRIMARY KEY, "+
				  KEY_TIME +" timestamp NOT NULL, "+
				  KEY_GROUP +" int NOT NULL, "+
				  KEY_SELF +" BOOLEAN NOT NULL DEFAULT false, "+
				  Messages.KEY_ID +" int DEFAULT NULL REFERENCES "+ Messages.NAME +" ON DELETE CASCADE ON UPDATE CASCADE, "+
				  KEY_PICTURE +" int DEAULT NULL, "+
				  Chatrooms.KEY_ID +" int NOT NULL REFERENCES "+ Chatrooms.NAME +" ON DELETE CASCADE ON UPDATE CASCADE)";
	}
	
    private static final class Messages {
    	public static final String NAME = "messages";
		public static final String KEY_ID = "msgID";
		public static final String KEY_MSG = "msg";
    	public static final String CREATE =
                "CREATE TABLE "+ NAME +" ("+
                KEY_ID +" int NOT NULL PRIMARY KEY, "+
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
		db.execSQL(Chatrooms.CREATE);
		db.execSQL(Messages.CREATE);
		db.execSQL(Chats.CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE "+ Chatrooms.NAME);
		db.execSQL("DROP TABLE "+ Chats.NAME);
		db.execSQL("DROP TABLE "+ Messages.NAME);
		
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
	
	@TargetApi(16)
	private void enableForeignKeysApi16(SQLiteDatabase db) {
		db.setForeignKeyConstraintsEnabled(true);
	}
	
	private void enableForeignKeysApi8(SQLiteDatabase db) {
		db.execSQL("PRAGMA foreign_keys=ON;");
	}

}