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
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

import java.util.List;
import java.util.Map;

import de.rallye.model.structures.Submission;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.model.structures.Task;
import de.stadtrallye.rallyesoft.uimodel.RallyeTabManager;
import de.stadtrallye.rallyesoft.uimodel.TaskCursorAdapter;
import de.stadtrallye.rallyesoft.uimodel.TaskCursorWrapper;

import static de.stadtrallye.rallyesoft.model.Model.getModel;
import static de.stadtrallye.rallyesoft.uimodel.TabManager.getTabManager;
import static de.stadtrallye.rallyesoft.uimodel.Util.getDefaultMapOptions;

/**
 * Fragment that contains a ViewPager sorting the Tasks in location specific and ubiquitous
 */
public class TasksOverviewFragment extends Fragment implements ITasks.ITasksListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

	private static final String THIS = ChatsFragment.class.getSimpleName();

	private IModel model;
	private ITasks tasks;
	private ListView list;
	private TaskCursorWrapper listAdapter;
	private byte size = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);
	}

	@TargetApi(11)
	private void setLayoutTransition(ViewGroup vg) {
		vg.setLayoutTransition(new LayoutTransition());
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(THIS, "we are in "+getClass().getName());
		Log.v(THIS, "got result: " + resultCode);
		if (resultCode== Activity.RESULT_OK) {

			Log.v(THIS, "need to update submissions");
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tasks_overview, container, false);

		list = (ListView) v.findViewById(R.id.tasks_list);

		if (android.os.Build.VERSION.SDK_INT >= 11)
			setLayoutTransition((ViewGroup) v);

		list.setOnItemClickListener(this);
		list.setOnItemLongClickListener(this);
//		list.setFastScrollEnabled(true);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();

		model = getModel(getActivity());
		tasks = model.getTasks();

		listAdapter = new TaskCursorWrapper(getActivity(), new TaskCursorAdapter(getActivity(), tasks.getTasksCursor()), tasks);
		list.setAdapter(listAdapter);

		FragmentManager fm = getChildFragmentManager();
		Fragment mapFragment = fm.findFragmentByTag(TasksMapFragment.TAG);

		if (mapFragment == null) {
			mapFragment = new TasksMapFragment();
			mapFragment.setArguments(getDefaultMapOptions(model));
			fm.beginTransaction().replace(R.id.map, mapFragment, TasksMapFragment.TAG).commit();
		}

		setSize(size);

		tasks.addListener(this);
		tasks.refreshSubmissions();
	}

	@Override
	public void onStop() {
		tasks.removeListener(this);

		super.onStop();

	}

	@Override
	public void onDetach() {
		super.onDetach();

		model = null; //are generally retained during Configuration changes, but since we are refreshing it anyway in onActivityCreated
		tasks = null; //same here
		listAdapter = null;
	}

//	@Override
//	public void onSaveInstanceState(Bundle outState) {
//		super.onSaveInstanceState(outState);
//
//	}

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
				model.getTasks().refresh();
				return true;
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
				list.setVisibility(View.GONE);
				break;
			case 2:
				list.setVisibility(View.VISIBLE);
				getView().findViewById(R.id.map).setVisibility(View.GONE);
				break;
			case 0:
				list.setVisibility(View.VISIBLE);
				getView().findViewById(R.id.map).setVisibility(View.VISIBLE);
		}
		size = newSize;
	}

	@Override
	public void taskUpdate() {
		listAdapter.changeCursor(tasks.getTasksCursor());
	}

	@Override
	public void submissionsUpdate(Map<Integer, List<Submission>> submissions) {

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//		Intent intent = new Intent(getActivity(), HostActivity.class);
//		intent.putExtra(Std.TASK_ID, (int) id);
//		startActivity(intent);

		Bundle args = new Bundle();
		args.putInt(Std.TASK_ID, (int)id);

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
}
