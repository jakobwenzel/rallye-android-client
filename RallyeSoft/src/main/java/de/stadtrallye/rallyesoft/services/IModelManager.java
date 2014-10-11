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

import de.rallye.model.calendar.Calendar;

/**
 * Created by Ramon on 17.09.2014.
 */
public interface IModelManager {

	boolean isCalendarPossible();
	boolean isCalendarAvailable();
	IHandle<Calendar> getCalendar();

	boolean isChatPossible();
	boolean isChatAvailable();
//	IHandle<ChatModel> getChat();

	boolean isRallyePossible();
	boolean isRallyeAvailable();
//	IHandle<RallyeModel> getRallye();

	boolean isModulePossible(int module);
	boolean isModuleCaches(int module);
	<T> IHandle<T> getModule(int module);

}
