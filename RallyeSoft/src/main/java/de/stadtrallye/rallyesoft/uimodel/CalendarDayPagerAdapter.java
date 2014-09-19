/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.rallye.model.calendar.ICalendar;
import de.stadtrallye.rallyesoft.fragments.CalendarDayFragment;

/**
 * Created by Ramon on 19.09.2014.
 */
public class CalendarDayPagerAdapter extends FragmentPagerAdapter {

	private ICalendar calendar;
	private final Context context;

	public CalendarDayPagerAdapter(FragmentManager fm, ICalendar calendar, Context context) {
		super(fm);
		this.calendar = calendar;
		this.context = context;
	}

	@Override
	public long getItemId(int position) {
		return calendar.getDay(position).getDayOfWeek();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return Util.resolveDayOfWeek(context, calendar.getDay(position).getDayOfWeek());
	}

	@Override
	public Fragment getItem(int i) {
		ICalendar.IDay day = calendar.getDay(i);


		return CalendarDayFragment.newInstance(i);
	}

	@Override
	public int getCount() {
		return calendar.getDayCount();
	}
}
