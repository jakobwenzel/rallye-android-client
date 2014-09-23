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

package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;

/**
 * Created by Ramon on 19.09.2014.
 */
public class CalendarDayFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.calendar_day_fragment, container, false);

		ListView lst_events = (ListView) v.findViewById(R.id.lst_events);

		return v;
	}



	public static CalendarDayFragment newInstance(int day) {
		CalendarDayFragment f = new CalendarDayFragment();
		Bundle args = new Bundle();
		args.putInt(Std.CALENDAR_DAY, day);
		f.setArguments(args);

		return f;
	}
}
