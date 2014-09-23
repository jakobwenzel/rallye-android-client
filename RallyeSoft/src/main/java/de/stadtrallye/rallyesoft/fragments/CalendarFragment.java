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
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import de.rallye.model.calendar.Calendar;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.services.IHandle;
import de.stadtrallye.rallyesoft.services.ModelManager;
import de.stadtrallye.rallyesoft.uimodel.CalendarDayPagerAdapter;

/**
 * Created by Ramon on 17.09.2014.
 */
public class CalendarFragment extends Fragment {

	private static final String THIS = CalendarFragment.class.getSimpleName();

	private ViewPager pager;
	private PagerSlidingTabStrip indicator;
	private CalendarDayPagerAdapter fragmentAdapter;
	private IHandle<Calendar> calendar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.calendar_fragment, container, false);

		pager = (ViewPager) v.findViewById(R.id.pager);
		pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin));
		indicator = (PagerSlidingTabStrip) v.findViewById(R.id.indicator);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedBundle) {
		super.onActivityCreated(savedBundle);

		calendar = ModelManager.getInstance().getCalendar();
	}

	//TODO
}
