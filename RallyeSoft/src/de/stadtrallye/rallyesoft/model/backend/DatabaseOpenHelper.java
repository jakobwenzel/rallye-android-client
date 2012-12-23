package de.stadtrallye.rallyesoft.model.backend;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "de.stadtrallye.rallyesoft.db";
  
	
    private static final class Messages {
    	public static final String NAME = "messages";
		private static final String KEY_MSG_ID = "msgID";
		private static final String KEY_MSG = "msg";
		private static final String KEY_PIC = "pic";
		private static final String KEY_CHATROOM = "chatroomID";
    	private static final String CREATE =
                "CREATE TABLE "+ NAME +" ("+
                KEY_MSG_ID +" INTEGER PRIMARY KEY, "+
                KEY_CHATROOM +" INTEGER, "+
                KEY_MSG + " TEXT, " +
                KEY_PIC + " INTEGER);";
    }

    public DatabaseOpenHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Messages.CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

}