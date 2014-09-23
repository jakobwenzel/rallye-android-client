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

package de.stadtrallye.rallyesoft.services;

import android.os.Binder;

import de.rallye.model.calendar.Calendar;

/**
 * Created by Ramon on 17.09.2014.
 */
public class ModelManager extends Binder implements IModelManager {

	public static ModelManager getInstance() {
		return new ModelManager();
	}

	public void onDestroy() {

	}

	public void cleanupMemory() {

	}

	@Override
	public boolean isCalendarPossible() {
		return false;
	}

	@Override
	public boolean isCalendarAvailable() {
		return false;
	}

	@Override
	public IHandle<Calendar> getCalendar() {
		return null;
	}

	@Override
	public boolean isChatPossible() {
		return false;
	}

	@Override
	public boolean isChatAvailable() {
		return false;
	}

	@Override
	public boolean isRallyePossible() {
		return false;
	}

	@Override
	public boolean isRallyeAvailable() {
		return false;
	}

	@Override
	public boolean isModulePossible(int module) {
		return false;
	}

	@Override
	public boolean isModuleCaches(int module) {
		return false;
	}

	@Override
	public <T> IHandle<T> getModule(int module) {
		return null;
	}
}
