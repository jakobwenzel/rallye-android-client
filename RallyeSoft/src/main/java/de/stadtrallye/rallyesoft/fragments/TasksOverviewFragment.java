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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.map.IMapManager;
import de.stadtrallye.rallyesoft.model.structures.Task;
import de.stadtrallye.rallyesoft.model.tasks.ITaskManager;
import de.stadtrallye.rallyesoft.threading.Threading;
import de.stadtrallye.rallyesoft.uimodel.RallyeTabManager;
import de.stadtrallye.rallyesoft.uimodel.TaskCursorAdapter;
import de.stadtrallye.rallyesoft.uimodel.TaskCursorWrapper;

import static de.stadtrallye.rallyesoft.uimodel.TabManager.getTabManager;
import static de.stadtrallye.rallyesoft.uimodel.Util.getDefaultMapOptions;

/**
 * Fragment that contains a ViewPager sorting the Tasks in location specific and ubiquitous
 */
public class TasksOverviewFragment extends Fragment implements ITaskManager.ITasksListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

	private static final String THIS = ChatsFragment.class.getSimpleName();

	private ListView list;
	private ViewGroup grp_map;

	private TaskCursorWrapper listAdapter;

	private ITaskManager taskManager;
	private IMapManager mapManager;

	private byte size = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);

		final Server server = Server.getCurrentServer();
		try {
			taskManager = server.acquireTaskManager(this);
			mapManager = server.acquireMapManager(this);
		} catch (NoServerKnownException e) {
			e.printStackTrace();
		}
	}

	@TargetApi(11)
	private void setLayoutTransition(ViewGroup vg) {
		vg.setLayoutTransition(new LayoutTransition());
	}

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tasks_overview, container, false);

		list = (ListView) v.findViewById(R.id.tasks_list);

		if (android.os.Build.VERSION.SDK_INT >= 11)
			setLayoutTransition((ViewGroup) v);

		list.setOnItemClickListener(this);
		list.setOnItemLongClickListener(this);
//		list.setFastScrollEnabled(true);

		grp_map = (ViewGroup) v.findViewById(R.id.map);

		if (savedInstanceState != null) {
			size = savedInstanceState.getByte(Std.SIZE, size);
		}
		setSize(size);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		listAdapter = new TaskCursorWrapper(getActivity(), new TaskCursorAdapter(getActivity(), taskManager.getTasksCursor()), taskManager);
		list.setAdapter(listAdapter);
	}

	@Override
	public void onStart() {
		super.onStart();

		FragmentManager fm = getChildFragmentManager();
		Fragment mapFragment = fm.findFragmentByTag(TasksMapFragment.TAG);

		if (mapFragment == null) {
			mapFragment = new TasksMapFragment();
			mapFragment.setArguments(getDefaultMapOptions(mapManager));
			fm.beginTransaction().replace(R.id.map, mapFragment, TasksMapFragment.TAG).commit();
		}

		taskManager.addListener(this);
	}

	@Override
	public void onStop() {
		taskManager.removeListener(this);

		super.onStop();

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putByte(Std.SIZE, size);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean drawerOpen = getTabManager(getActivity()).isMenuOpen();

		menu.findItem(R.id.refresh_menu).setVisible(!drawerOpen);
		menu.findItem(R.id.resize_menu).setVisible(!drawerOpen);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem refreshMenuItem = menu.add(Menu.NONE, R.id.refresh_menu, 30, R.string.refresh);
		refreshMenuItem.setIcon(R.drawable.ic_refresh_light);
		refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		MenuItem resize = menu.add(Menu.NONE, R.id.resize_menu, 40, R.string.resize);
		resize.setIcon(R.drawable.ic_center_light);
		resize.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh_menu:
				try {
					taskManager.update();
				} catch (NoServerKnownException e) {
					Toast.makeText(getActivity(), R.string.notConnected, Toast.LENGTH_SHORT).show();
				}
				return true;
			case R.id.resize_menu:
				setSize((byte)((size+1) % 3));
				return true;
			default:
				Log.d(THIS, "No hit on menu item " + item);
				return false;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		taskManager.removeListener(this);
		taskManager = null;
		listAdapter.close();
	}

	private void setSize(byte newSize) {
		switch (newSize) {
			case 1:
				list.setVisibility(View.GONE);
				break;
			case 2:
				list.setVisibility(View.VISIBLE);
				grp_map.setVisibility(View.GONE);
				break;
			case 0:
				list.setVisibility(View.VISIBLE);
				grp_map.setVisibility(View.VISIBLE);
		}
		size = newSize;
	}

	@Override
	public void taskUpdate() {
		listAdapter.changeCursor(taskManager.getTasksCursor());
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//		Intent intent = new Intent(getActivity(), HostActivity.class);
//		intent.putExtra(Std.TASK_ID, (int) id);
//		startActivity(intent);

		Bundle args = new Bundle();
		args.putInt(Std.TASK_ID, (int)id);
		args.putInt(Std.POSITION, listAdapter.translatePosition(position));

		getTabManager(getActivity()).openSubTab(RallyeTabManager.TAB_TASKS_DETAILS, args);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		Toast.makeText(getActivity(), getSubmitHint(listAdapter.getTask(position)), Toast.LENGTH_SHORT).show();
		return true;
	}

	private String getSubmitHint(Task task) {
		int resId;

		switch (task.submits) {
			case Task.SUBMITS_COMPLETE:
				resId = R.string.solution_submitted;
				break;
			case Task.SUBMITS_SOME:
				resId = R.string.solutions_submitted;
				break;
			case Task.SUBMITS_NONE:
				resId = R.string.no_solution_submitted;
				break;
			case Task.SUBMITS_UNKNOWN:
			default:
				resId = R.string.solution_state_unknown;
		}
		return getString(resId);
	}

	@Override
	public Handler getCallbackHandler() {
		return Threading.getUiExecutor();
	}
}
