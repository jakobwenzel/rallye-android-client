package de.stadtrallye.rallyesoft.fragments;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.model.structures.LatLngAdapter;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.TaskCursorAdapter;
import de.stadtrallye.rallyesoft.widget.MapPager;

/**
 * Fragment that contains a ViewPager sorting the Tasks in location specific and ubiquitous
 */
public class TasksOverviewFragment extends SherlockFragment implements ITasks.ITasksListener, AdapterView.OnItemClickListener {

	private static final String THIS = ChatsFragment.class.getSimpleName();

	private IModel model;
	private ITasks tasks;
	private ListView list;
//	private View map;
	private TaskCursorAdapter listAdapter;
	private byte size = 0;//TODO: save

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tasks_overview, container, false);

		list = (ListView) v.findViewById(R.id.tasks_list);

		if (android.os.Build.VERSION.SDK_INT >= 11)
			setLayoutTransition(container);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			model = ((IModelActivity) getActivity()).getModel();
			tasks = model.getTasks();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity and extend SlidingFragmentActivity");
		}

		listAdapter = new TaskCursorAdapter(getActivity(), tasks.getTasksCursor());
		list.setAdapter(listAdapter);

		list.setOnItemClickListener(this);

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

		tasks.addListener(this);
	}

	@Override
	public void onStop() {
		tasks.removeListener(this);

		super.onStop();

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem refreshMenuItem = menu.add(Menu.NONE, R.id.refresh_menu, 30, R.string.refresh);

		refreshMenuItem.setIcon(R.drawable.refresh);
		refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		MenuItem resize = menu.add(Menu.NONE, R.id.resize_menu, 40, R.string.resize);

		resize.setIcon(R.drawable.center);
		refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh_menu:
				model.getTasks().updateTasks();
				return true;
			case R.id.resize_menu:
				switch (size) {
					case 0:
						list.setVisibility(View.GONE);
						size = 1;
						break;
					case 1:
						list.setVisibility(View.VISIBLE);
						getView().findViewById(R.id.map).setVisibility(View.GONE);
						size = 2;
						break;
					case 2:
						list.setVisibility(View.VISIBLE);
						getView().findViewById(R.id.map).setVisibility(View.VISIBLE);
						size = 0;
				}
				return true;
			default:
				Log.d(THIS, "No hit on menu item " + item);
				return false;
		}
	}

	@Override
	public void taskUpdate() {
		listAdapter.changeCursor(tasks.getTasksCursor());
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		Fragment f = fm.findFragmentByTag("taskPager");
		if (f == null) {
			f = new TasksPagerFragment();
		}

		Bundle b = new Bundle();
		b.putInt(Std.TASK_CURSOR_POSITION, position);
		f.setArguments(b);

		ft.addToBackStack("SubFrag").replace(android.R.id.content, f, "taskPager").commit();
	}
}
