package de.stadtrallye.rallyesoft.model.db;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 15;
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
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
					KEY_ID +" INTEGER PRIMARY KEY, "+
					KEY_NAME +" VARCHAR(50) NOT NULL, "+
					KEY_LAST_UPDATE +" TIMESTAMP, "+
					KEY_LAST_ID +" INTEGER)";
	}
  
	public static final class Chats {
		public static final String TABLE = "chats";
		public static final String KEY_ID = "chatID";
		public static final String KEY_TIME = "timestamp";
		public static final String FOREIGN_USER = "userID";
		public static final String KEY_PICTURE = "pictureID";
		public static final String FOREIGN_GROUP = Groups.KEY_ID;
		public static final String FOREIGN_MSG = Messages.KEY_ID;
		public static final String FOREIGN_ROOM = Chatrooms.KEY_ID;
		public static final String CREATE =
				"CREATE TABLE "+ TABLE +" ("+
				  KEY_ID +" INTEGER PRIMARY KEY, "+
				  KEY_TIME +" TIMESTAMP NOT NULL, "+
				  Groups.KEY_ID +" INTEGER NOT NULL REFERENCES "+ Groups.TABLE +" ON DELETE CASCADE ON UPDATE CASCADE, "+
				  FOREIGN_USER +" INTEGER NOT NULL, "+
				  Messages.KEY_ID +" INTEGER DEFAULT NULL REFERENCES "+ Messages.TABLE +" ON DELETE CASCADE ON UPDATE CASCADE, "+
				  KEY_PICTURE +" INTEGER, "+
				  Chatrooms.KEY_ID +" INTEGER NOT NULL REFERENCES "+ Chatrooms.TABLE +" ON DELETE CASCADE ON UPDATE CASCADE)";
		public static final String[] COLS = new String[]{ Chats.KEY_ID, Chats.KEY_TIME, Chats.FOREIGN_GROUP, Chats.FOREIGN_USER, Chats.FOREIGN_MSG, Chats.KEY_PICTURE, Chats.FOREIGN_ROOM };
	}
	
    public static final class Messages {
    	public static final String TABLE = "messages";
		public static final String KEY_ID = "msgID";
		public static final String KEY_MSG = "msg";
    	public static final String CREATE =
                "CREATE TABLE "+ TABLE +" ("+
                KEY_ID +" INTEGER PRIMARY KEY, "+
                KEY_MSG + " TEXT)";
    	public static final String[] COLS = new String[]{ Messages.KEY_ID, Messages.KEY_MSG };
    }
    
    public static final class Nodes {
    	public static final String TABLE = "nodes";
    	public static final String KEY_ID = "nodeID";
    	public static final String KEY_NAME = "name";
    	public static final String KEY_LAT = "latitude";
    	public static final String KEY_LON = "longitude";
    	public static final String KEY_DESCRIPTION = "description";
    	public static final String CREATE =
    			"CREATE TABLE "+ TABLE +" ("+
    					KEY_ID +" int PRIMARY KEY, "+
    					KEY_NAME +" VARCHAR(50) NOT NULL, "+
    					KEY_LAT +" double NOT NULL, "+
    					KEY_LON +" double NOT NULL, "+
    					KEY_DESCRIPTION +" TEXT)";
    	public static final String[] COLS = new String[]{ Nodes.KEY_ID, Nodes.KEY_NAME, Nodes.KEY_LAT, Nodes.KEY_LON, Nodes.KEY_DESCRIPTION };
    }
    
    public static final class Edges {
    	public static final String TABLE = "edges";
    	public static final String KEY_A = "aNodeID";
    	public static final String KEY_B = "bNodeID";
    	public static final String KEY_TYPE = "type";
    	public static final String CREATE =
    			"CREATE TABLE "+ TABLE +" ("+
    					KEY_A +" int NOT NULL REFERENCES "+ Nodes.TABLE +" ON DELETE CASCADE ON UPDATE CASCADE, "+
    					KEY_B +" int NOT NULL REFERENCES "+ Nodes.TABLE +" ON DELETE CASCADE ON UPDATE CASCADE, "+
    					KEY_TYPE +" VARCHAR(10) NOT NULL, "+
    					"PRIMARY KEY ("+ KEY_A+", "+ KEY_B +"))";
    	public static final String[] COLS = new String[]{ Edges.KEY_A, Edges.KEY_B, Edges.KEY_TYPE };
    }
    
//    private static final class Pictures {
//    	public static final String NAME = "pictures";
//    	public static final String KEY_ID = "pictureID";
//    	public static final String CREATE =
//    			"CREATE TABLE "+ NAME +" ("+
//    					KEY_ID +" int AUTOINCREMENT PRIMARY KEY,"+
//    					")";
//    }

    public DatabaseHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Groups.CREATE);
		db.execSQL(Chatrooms.CREATE);
		db.execSQL(Messages.CREATE);
		db.execSQL(Chats.CREATE);
		db.execSQL(Nodes.CREATE);
		db.execSQL(Edges.CREATE);
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
		db.execSQL("DROP TABLE IF EXISTS "+ Nodes.TABLE);
		db.execSQL("DROP TABLE IF EXISTS "+ Edges.TABLE);
		
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

}