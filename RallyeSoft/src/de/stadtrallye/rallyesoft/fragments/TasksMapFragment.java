package de.stadtrallye.rallyesoft.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.AdapterView;

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

import static de.stadtrallye.rallyesoft.model.structures.LatLngAdapter.toGms;

/**
 * Map of all location specific tasks, combined with a list for direct selection
 * Possibly ability to maximize the map to fullsize
 */
public class TasksMapFragment extends SherlockMapFragment implements GoogleMap.OnMarkerClickListener, ITasks.ITasksListener, GoogleMap.OnInfoWindowClickListener {

	private IModel model;
	private ITasks tasks;
	private GoogleMap gmap;
	private HashMap<Marker, Integer> markers = new HashMap<>();

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

//		if (tasks.getMapLocation() != null)
//			gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(toGms(tasks.getMapLocation()), tasks.getZoomLevel()));

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
		populateMap();
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		int id = markers.get(marker);//TODO: show details
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		Fragment f = fm.findFragmentByTag("taskPager");
		if (f == null) {
			f = new TasksPagerFragment();
		}

		Bundle b = new Bundle();
		b.putInt(Std.TASK_CURSOR_POSITION, id);
		f.setArguments(b);

		ft.addToBackStack("SubFrag").replace(android.R.id.content, f, "taskPager");
	}
}