package de.stadtrallye.rallyesoft.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.viewpagerindicator.TitlePageIndicator;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.ITasksMapControl;

import static de.stadtrallye.rallyesoft.uimodel.Util.getDefaultMapOptions;

/**
 * Fragment to show the details of tasks, similar to the mail-view in GMail
 * Enhanced by a GMap
 */
public class TasksPagerFragment extends SherlockFragment implements ITasks.ITasksListener {

	private static final String THIS = TasksPagerFragment.class.getSimpleName();

	private IModel model;
	private ViewPager pager;
	private TaskFragmentAdapter fragmentAdapter;
	private ITasks tasks;
	private ITasksMapControl mapControl;
	private TitlePageIndicator indicator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		if (savedInstanceState != null)
//			currentTab = savedInstanceState.getInt(Std.TAB);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tasks_pager, container, false);

		pager = (ViewPager) v.findViewById(R.id.tasks_pager);
		indicator = (TitlePageIndicator) v.findViewById(R.id.pager_indicator);

		pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin));

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			model = ((IModelActivity) getActivity()).getModel();
			tasks = model.getTasks();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity");
		}

		fragmentAdapter = new TaskFragmentAdapter(getChildFragmentManager(), tasks.getTasksCursor());
		pager.setAdapter(fragmentAdapter);
		indicator.setViewPager(pager);

		int tab = -1;
		if (savedInstanceState != null) {
			tab = savedInstanceState.getInt(Std.TAB, -1);
		}
		Bundle args = getArguments();
		if (tab == -1 && args != null) {
			int id = args.getInt(Std.TASK_ID, -1);
			tab = (id >= 0) ? tasks.getTaskPositionInCursor(id) : 0;
		}

		pager.setCurrentItem(tab);

		FragmentManager fm = getChildFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		Fragment mapFragment = fm.findFragmentByTag(TasksMapFragment.TAG);
		if (mapFragment == null) {
			mapFragment = new TasksMapFragment();

			args = getDefaultMapOptions(model);
			args.putBoolean(Std.TASK_MAP_MODE_SINGLE, true);
			mapFragment.setArguments(args);
		}

		mapControl = (ITasksMapControl) mapFragment;

		ft.replace(R.id.map, mapFragment, TasksMapFragment.TAG).commit();
	}

	@Override
	public void onStart() {
		super.onStart();

		tasks.addListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		tasks.removeListener(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(Std.TAB, pager.getCurrentItem());
	}

	@Override
	public void taskUpdate() {
		fragmentAdapter.changeCursor(tasks.getTasksCursor());
	}

	private class TaskFragmentAdapter extends FragmentStatePagerAdapter {

		private Cursor cursor;

		private CursorConverters.TaskCursorIds c;

		public TaskFragmentAdapter(FragmentManager fm, Cursor cursor) {
			super(fm);
			this.cursor = cursor;

			c = CursorConverters.TaskCursorIds.read(cursor);
		}

		public void changeCursor(Cursor cursor) {
			Cursor old = this.cursor;
			if (old != null) {
				old.close();
			}
			c = CursorConverters.TaskCursorIds.read(cursor);
			this.cursor = cursor;
			this.notifyDataSetChanged();
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object) {
			super.setPrimaryItem(container, position, object);

			mapControl.setTask(CursorConverters.getTask(position, cursor, c));
		}

		@Override
		public Fragment getItem(int position) {
			Fragment f = new TaskDetailsFragment();
			Bundle args = new Bundle();
			args.putSerializable(Std.TASK, CursorConverters.getTask(position, cursor, c));
			f.setArguments(args);
			return f;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return (position+1) +" "+ getString(R.string.of) +" "+ getCount();
		}

		@Override
		public int getCount() {
			return cursor.getCount();
		}
	}
}
