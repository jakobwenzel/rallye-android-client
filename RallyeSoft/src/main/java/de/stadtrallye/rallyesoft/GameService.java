package de.stadtrallye.rallyesoft;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GameService extends Service{
	

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
