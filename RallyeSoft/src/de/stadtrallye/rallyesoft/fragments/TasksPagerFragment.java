package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.viewpagerindicator.TitlePageIndicator;

import java.util.List;
import java.util.Map;

import de.rallye.model.structures.Submission;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.ITasksMapControl;
import de.stadtrallye.rallyesoft.uimodel.TaskPagerAdapter;

import static de.stadtrallye.rallyesoft.uimodel.Util.getDefaultMapOptions;

/**
 * Fragment to show the details of tasks, similar to the mail-view in GMail
 * Enhanced by a GMap
 */
public class TasksPagerFragment extends SherlockFragment implements ITasks.ITasksListener {

	private static final String THIS = TasksPagerFragment.class.getSimpleName();

	private IModel model;
	private ViewPager pager;
	private TaskPagerAdapter fragmentAdapter;
	private ITasks tasks;
	private ITasksMapControl mapControl;
	private TitlePageIndicator indicator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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

		FragmentManager fm = getChildFragmentManager();
		Fragment mapFragment = fm.findFragmentByTag(TasksMapFragment.TAG);
		if (mapFragment == null) {

			mapFragment = new TasksMapFragment();

			Bundle args = getDefaultMapOptions(model);
			args.putBoolean(Std.TASK_MAP_MODE_SINGLE, true);
			mapFragment.setArguments(args);
			fm.beginTransaction().replace(R.id.map, mapFragment, TasksMapFragment.TAG).commit();
		}

		mapControl = (ITasksMapControl) mapFragment;

		fragmentAdapter = new TaskPagerAdapter(getChildFragmentManager(), getActivity(), tasks.getTasksCursor(), mapControl);
		pager.setAdapter(fragmentAdapter);
		indicator.setViewPager(pager); // Needs an adapter

		Bundle args = getArguments();
		if (args != null) {
			int id = args.getInt(Std.TASK_ID, -1);
			int tab = (id >= 0) ? tasks.getTaskPositionInCursor(id) : 0;
			pager.setCurrentItem(tab);
		}
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
	public void onDetach() {
		super.onDetach();

		model = null; // Just to be sure, since we load the model anyway in onActivityCreated
		tasks = null;
		fragmentAdapter = null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

	}

	@Override
	public void taskUpdate() {
		fragmentAdapter.changeCursor(tasks.getTasksCursor());
	}

	@Override
	public void submissionsUpdate(Map<Integer, List<Submission>> submissions) {

	}
}
