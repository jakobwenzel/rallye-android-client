package de.stadtrallye.rallyesoft.fragments;

import android.database.Cursor;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

import de.rallye.model.structures.Task;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.ITabActivity;
import de.stadtrallye.rallyesoft.uimodel.ITasksMapControl;
import de.stadtrallye.rallyesoft.uimodel.RallyeTabManager;
import de.stadtrallye.rallyesoft.uimodel.TabManager;

import static de.stadtrallye.rallyesoft.model.structures.LatLngAdapter.toGms;

/**
 * Map of all location specific tasks, combined with a list for direct selection
 * Possibly ability to maximize the map to fullsize
 */
public class TasksMapFragment extends SherlockMapFragment implements GoogleMap.OnMarkerClickListener, ITasks.ITasksListener, GoogleMap.OnInfoWindowClickListener,
																		ITasksMapControl {

	public static final String TAG = "taskMap";

	private IModel model;
	private ITasks tasks;
	private GoogleMap gmap;
	private HashMap<Marker, Integer> markers = new HashMap<>();
	private boolean singleMode;
	private TabManager tabManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			model = ((IModelActivity) getActivity()).getModel();
			tasks = model.getTasks();
			tabManager = ((ITabActivity) getActivity()).getTabManager();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity");
		}
	}

	@Override
	public void onStart() {
		super.onStart();


		if (gmap == null) {
			gmap = getMap();
			gmap.setMyLocationEnabled(true);
//			gmap.setOnMarkerClickListener(this);
//			gmap.setOnMapClickListener(this);
//			gmap.setOnCameraChangeListener(this);
			gmap.setOnInfoWindowClickListener(this);
		}

		singleMode = getArguments().getBoolean(Std.TASK_MAP_MODE_SINGLE, false);

//		if (tasks.getMapLocation() != null)
//			gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(toGms(tasks.getMapLocation()), tasks.getZoomLevel()));

		if (!singleMode)
			populateMap();
	}

	@Override
	public void onStop() {
		super.onStop();

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

	}

	public void populateMap() {

		markers.clear();
		gmap.clear();

		Task t;
		Cursor cursor = tasks.getTasksCursor();
		CursorConverters.TaskCursorIds c = CursorConverters.TaskCursorIds.read(cursor);

		for (int i = 0; i < cursor.getCount(); i++) {
			t = CursorConverters.getTask(i, cursor, c);
			if (!t.hasLocation())
				continue;
			Marker m = gmap.addMarker(new MarkerOptions()
					.position(toGms(t.location))
					.title(t.name)
					.snippet(t.description));
			markers.put(m, t.taskID);
//			markers.add(m);
		}
	}


	@Override
	public boolean onMarkerClick(Marker marker) {
		marker.showInfoWindow();
		/*
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
			Uri.parse("http://maps.google.com/maps?saddr="+ starting_point_lat+","+starting_point_long+"&daddr="+dest_point_lat+","+dest_point_long+""));
			startActivity(intent);
		*/

		return false;
	}

	@Override
	public void taskUpdate() {
		if (!singleMode)
			populateMap();
	}

	@Override
	public void setTask(Task task) {
		gmap.clear();

		if (!task.hasLocation())
			return;

		gmap.addMarker(new MarkerOptions()
				.position(toGms(task.location))
				.title(task.name)
				.snippet(task.description));

		gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(toGms(task.location), model.getMap().getZoomLevel() + 4));
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		int id = markers.get(marker);

//		Intent intent = new Intent(getActivity(), HostActivity.class);
//		intent.putExtra(Std.TASK_ID, id);
//		startActivity(intent);

		Bundle args = new Bundle();
		args.putInt(Std.TASK_ID, id);

		tabManager.openSubTab(RallyeTabManager.TAB_TASKS_DETAILS, args);
	}
}