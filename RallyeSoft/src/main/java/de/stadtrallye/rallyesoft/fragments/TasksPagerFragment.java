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

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.viewpagerindicator.TitlePageIndicator;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.map.IMapManager;
import de.stadtrallye.rallyesoft.model.tasks.ITaskManager;
import de.stadtrallye.rallyesoft.threading.Threading;
import de.stadtrallye.rallyesoft.uimodel.ITasksMapControl;
import de.stadtrallye.rallyesoft.uimodel.TaskPagerAdapter;

import static de.stadtrallye.rallyesoft.uimodel.Util.getDefaultMapOptions;

/**
 * Fragment to shows the details of tasks, similar to the mail-view in GMail
 * Enhanced by a GMap
 */
public class TasksPagerFragment extends Fragment implements ITaskManager.ITasksListener {

	private static final String THIS = TasksPagerFragment.class.getSimpleName();
	public static final String ARG_POS = Std.POSITION;
	public static final String ARG_TASK_ID = Std.TASK_ID;

	private ViewPager pager;
	private TitlePageIndicator indicator;

	private TaskPagerAdapter fragmentAdapter;

	private ITaskManager taskManager;
	private IMapManager mapManager;
//	private ITasksMapControl mapControl;

	private byte size = 0;

//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.v(THIS, "we are in "+getClass().getName());
//		Log.v(THIS, "got result: " + resultCode);
//		if (resultCode== Activity.RESULT_OK) {
//
//			Log.v(THIS, "need to update submissions");
//		}
//	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		setRetainInstance(true);
		setHasOptionsMenu(true);

		final Server server = Server.getCurrentServer();
		taskManager = server.acquireTaskManager(this);
		mapManager = server.acquireMapManager(this);
	}

	@TargetApi(11)
	private void setLayoutTransition(ViewGroup vg) {
		vg.setLayoutTransition(new LayoutTransition());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tasks_pager, container, false);

		pager = (ViewPager) v.findViewById(R.id.tasks_pager);
		indicator = (TitlePageIndicator) v.findViewById(R.id.pager_indicator);

		pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin));

		if (android.os.Build.VERSION.SDK_INT >= 11)
			setLayoutTransition((ViewGroup) v);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		FragmentManager fm = getChildFragmentManager();
		Fragment mapFragment = fm.findFragmentByTag(TasksMapFragment.TAG);
		if (mapFragment == null) {

			mapFragment = new TasksMapFragment();

			Bundle args = getDefaultMapOptions(mapManager);
			args.putBoolean(Std.TASK_MAP_MODE_SINGLE, true);
			mapFragment.setArguments(args);
			fm.beginTransaction().replace(R.id.map, mapFragment, TasksMapFragment.TAG).commit();
		}

		ITasksMapControl mapControl = (ITasksMapControl) mapFragment;

		fragmentAdapter = new TaskPagerAdapter(fm, getActivity(), taskManager, mapControl);
		pager.setAdapter(fragmentAdapter);
		indicator.setViewPager(pager); // Needs an adapter

		Bundle args = getArguments();
		if (args != null) {
			int id = args.getInt(ARG_TASK_ID, -1);
			int pos = args.getInt(ARG_POS, 0);
			int page;
			if (id > 0) {
				if (fragmentAdapter.isPosMatchingID(pos, id))
					page = pos;
				else
					page = fragmentAdapter.getPositionInCursor(id);
			} else {
				page = 0;
			}
			pager.setCurrentItem(page);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		taskManager.addListener(this);
		setSize(size);
	}

	@Override
	public void onStop() {
		super.onStop();

		taskManager.removeListener(this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem resize = menu.add(Menu.NONE, R.id.resize_menu, 40, R.string.resize);
		resize.setIcon(R.drawable.ic_center_light);
		resize.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.resize_menu:
				setSize((byte)((size+1) % 3));
				return true;
			default:
				Log.d(THIS, "No hit on menu item " + item);
				return false;
		}
	}

	private void setSize(byte newSize) {
		switch (newSize) {
			case 1:
				pager.setVisibility(View.GONE);
				indicator.setVisibility(View.GONE);
				break;
			case 2:
				pager.setVisibility(View.VISIBLE);
				indicator.setVisibility(View.VISIBLE);
				getView().findViewById(R.id.map).setVisibility(View.GONE);
				break;
			case 0:
				pager.setVisibility(View.VISIBLE);
				indicator.setVisibility(View.VISIBLE);
				getView().findViewById(R.id.map).setVisibility(View.VISIBLE);
		}
		size = newSize;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		final Server server = Server.getCurrentServer();
		server.releaseTaskManager(this);
		server.releaseMapManager(this);
		taskManager = null;
		mapManager = null;
//		fragmentAdapter.close();
	}

	@Override
	public void taskUpdate() {
		fragmentAdapter.changeCursor(taskManager.getTasksCursor());
	}

	@Override
	public Handler getCallbackHandler() {
		return Threading.getUiExecutor();
	}
}
