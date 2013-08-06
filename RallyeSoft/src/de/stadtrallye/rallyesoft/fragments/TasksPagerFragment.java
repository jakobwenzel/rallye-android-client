package de.stadtrallye.rallyesoft.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.CameraPosition;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.structures.LatLngAdapter;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.widget.MapPager;

/**
 * Created by Ramon on 06.08.13.
 */
public class TasksPagerFragment extends SherlockFragment implements ITasks.ITasksListener {

	private static final String THIS = TasksPagerFragment.class.getSimpleName();

	private IModel model;
	private ViewPager pager;
	private TaskFragmentAdapter fragmentAdapter;
	private ITasks tasks;
//	private int currentTab = 0;
//	private SlidingMenu slidingMenu;
//	private SlidingMenuHelper slidingHelper = new SlidingMenuHelper();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		if (savedInstanceState != null)
//			currentTab = savedInstanceState.getInt(Std.TAB);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tasks_pager, container, false);

		pager = (ViewPager) v.findViewById(R.id.tasks_pager);
		pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin));
//		indicator = (PagerSlidingTabStrip) v.findViewById(R.id.indicator);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			model = ((IModelActivity) getActivity()).getModel();
			tasks = model.getTasks();
//			slidingMenu = ((SlidingFragmentActivity) getActivity()).getSlidingMenu();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity and extend SlidingFragmentActivity");//TODO
		}

		fragmentAdapter = new TaskFragmentAdapter(getChildFragmentManager(), tasks.getTasksCursor());
		pager.setAdapter(fragmentAdapter);

		if (savedInstanceState != null) {
			pager.setCurrentItem(savedInstanceState.getInt(Std.TASK_CURSOR_POSITION, 0));
		}

		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		Fragment mapFragment = fm.findFragmentByTag("taskMap");
		if (mapFragment == null) {
			mapFragment = new TasksMapFragment();
			Bundle b = new Bundle();
			GoogleMapOptions gmo = new GoogleMapOptions().compassEnabled(true);
			de.rallye.model.structures.LatLng loc = model.getMap().getMapLocation();
			if (loc != null) {
				gmo.camera(new CameraPosition(LatLngAdapter.toGms(loc), model.getMap().getZoomLevel(), 0, 0));
			}
			b.putParcelable("MapOptions", gmo);
			mapFragment.setArguments(b);
		}

		ft.replace(R.id.map, mapFragment, "taskMap").commit();
	}

	@Override
	public void onStart() {
		super.onStart();


//		pager.setCurrentItem(currentTab);
		tasks.addListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		tasks.removeListener(this);

//		currentTab = pager.getCurrentItem();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

//		outState.putInt(Std.TAB, pager.getCurrentItem());
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
		public Fragment getItem(int position) {
			Fragment f = new TaskDetailsFragment();
			Bundle args = new Bundle();
			args.putSerializable(Std.TASK, CursorConverters.getTask(position, cursor, c));
			f.setArguments(args);
			return f;
		}


		@Override
		public int getCount() {
			return cursor.getCount();
		}
	}
}
