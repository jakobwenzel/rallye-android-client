package de.stadtrallye.rallyesoft.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rallye.model.structures.MapConfig;
import de.rallye.model.structures.Submission;
import de.rallye.model.structures.Task;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.SherlockMapFragment;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.ITasksMapControl;
import de.stadtrallye.rallyesoft.uimodel.RallyeTabManager;
import de.stadtrallye.rallyesoft.uimodel.TabManager;

import static de.stadtrallye.rallyesoft.model.structures.LatLngAdapter.toGms;
import static de.stadtrallye.rallyesoft.uimodel.TabManager.getTabManager;

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
	private final HashMap<Marker, Integer> markers = new HashMap<Marker, Integer>();
	private boolean singleMode;
	private TabManager tabManager;
	private boolean isLayouted;

//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
//	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		isLayouted = false;
		v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				isLayouted = true;
			}
		});
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			model = ((IModelActivity) getActivity()).getModel();
			tasks = model.getTasks();
			tabManager = getTabManager(getActivity());
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity and ITabActivity");
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

		if (!singleMode) {
			populateMap();
			tasks.addListener(this);
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		tasks.removeListener(this);
	}

//	@Override
//	public void onSaveInstanceState(Bundle outState) {
//		super.onSaveInstanceState(outState);
//
//	}

	private void populateMap() {

		markers.clear();
		gmap.clear();

		Task t;
		Cursor cursor = tasks.getTasksCursor();
		CursorConverters.TaskCursorIds c = CursorConverters.TaskCursorIds.read(cursor);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
		//for (int i = 0; i < cursor.getCount(); i++) {
			t = CursorConverters.getTask(cursor, c);
			cursor.moveToNext();
			if (!t.hasLocation())
				continue;

			Marker m = plotTask(t);

			markers.put(m, t.taskID);
		}
	}


	private Marker plotTask(Task t) {
		LatLng position = toGms(t.location);

		Marker m = gmap.addMarker(new MarkerOptions()
				.position(position)
				.title(t.name)
				.snippet(t.description));

		if (t.radius > 0) {
			gmap.addCircle(new CircleOptions()
					.center(position)
					.strokeWidth(1)
					.fillColor(0x80FF80A0)
					.radius(t.radius));
		}

		return m;
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
	public void submissionsUpdate(Map<Integer, List<Submission>> submissions) {

	}

	@Override
	public void setTask(Task task) {
		gmap.clear();

		if (!task.hasLocation())
			return;

		plotTask(task);

		LatLng coords = toGms(task.location);

		final CameraUpdate cu;

		if (task.radius > 0) {
			final double kmPerLat = 110.6 * 1000; // 1 Latitudinal Degree is about 110.6 km
			final double kmLat = coords.latitude * kmPerLat;
			final double radiusLat1 = (kmLat + task.radius) / kmPerLat; // calculate the roundabout coordinates of a point on the radius in latitudinal direction
			final double radiusLat2 = (kmLat - task.radius) / kmPerLat; // now in the other direction
			LatLng rad1 = new LatLng(radiusLat1, coords.longitude); //NOTE: we only define a line from north to south as bounds, as long as the maps width is greater than the height this will work out
			LatLng rad2 = new LatLng(radiusLat2, coords.longitude); //NOTE: longitudinal degrees vary greatly in distance depending on how close one is to the equator!

			int padding = getResources().getDimensionPixelOffset(R.dimen.map_center_padding);

			cu = CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().include(rad1).include(rad2).build(), padding);
		} else {
			MapConfig mapConfig = model.getMap().getMapConfig();
			cu = CameraUpdateFactory.newLatLngZoom(coords, (mapConfig != null)? mapConfig.zoomLevel + 4 : 16);
		}

		if (isLayouted)
			gmap.animateCamera(cu);
		else {
			getView().post(new Runnable() { // MapView has not been layouted yet (newLatLngBounds will throw Exception), so post it in the queue
				@Override
				public void run() {
					gmap.animateCamera(cu);
				}
			});
		}
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		if (singleMode)
			return;

		int id = markers.get(marker);

//		Intent intent = new Intent(getActivity(), HostActivity.class);
//		intent.putExtra(Std.TASK_ID, id);
//		startActivity(intent);

		Bundle args = new Bundle();
		args.putInt(Std.TASK_ID, id);

		tabManager.openSubTab(RallyeTabManager.TAB_TASKS_DETAILS, args);
	}
}