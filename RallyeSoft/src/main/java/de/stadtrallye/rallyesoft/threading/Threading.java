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

package de.stadtrallye.rallyesoft.threading;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Ramon on 25.09.2014.
 */
public class Threading {

	private static Handler uiHandler;
	private static ExecutorService workExecutor;

	public static Handler getUiExecutor() {
		if (uiHandler == null) {
			uiHandler = new Handler(Looper.getMainLooper());
		}
		return uiHandler;
	}

	public static ExecutorService getNetworkExecutor() {
		if (workExecutor == null) {
			workExecutor = Executors.newCachedThreadPool();
		}
		return workExecutor;
	}
}
