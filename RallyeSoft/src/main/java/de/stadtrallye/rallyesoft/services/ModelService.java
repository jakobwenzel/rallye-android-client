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
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * This Service should manage all parts of the server, so they can stay alive during configuration changes or in background
 *
 * For extra efficiency this service should be started and bound
 *
 * It will terminate itself after some time, if not needed anymore (at least long enough so it does not terminate during configuration changes)
 */
public class ModelService extends Service{

	private static final String THIS = ModelService.class.getSimpleName();
	private ModelManager manager;
	private boolean externalUse;

	@Override
	public void onCreate() {
		super.onCreate();

		manager = ModelManager.getInstance();

	}



	@Override
	public IBinder onBind(Intent intent) {
		externalUse = true;

		return manager;
	}

	/**
	 * All clients unbound, meaning that technically, we could free up everything
	 *
	 * This will happen during configuration changes, so we should not do anything then
	 *
	 * Relies on TrimRequests from the system and not time. Goal: keep all data as long as the system does not need the resources
	 * @param intent
	 * @return
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		externalUse = false;

		return false;
	}

	/**
	 * Cleanup more aggressively if we are unbound
	 * @param level
	 */
	@Override
	public void onTrimMemory(int level) {
		Log.i(THIS, "onTrimMemory: "+ level +", externalUse: "+ externalUse);

		//manager.cleanupXXX();
		// Transmit as much information as possible, so that the choice what and when to dismiss data is made in ModelManager
		// Possibly simplify level
	}

	@Override
	public void onLowMemory() {
		if (!externalUse)
			this.stopSelf();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		manager.onDestroy();
		manager = null;
	}
}
