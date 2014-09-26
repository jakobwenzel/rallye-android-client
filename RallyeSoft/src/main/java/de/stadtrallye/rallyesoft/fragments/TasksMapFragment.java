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

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.MapConfig;
import de.rallye.model.structures.Node;
import de.rallye.model.structures.Task;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.tasks.ITaskManager;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;
import de.stadtrallye.rallyesoft.model.map.IMapManager;
import de.stadtrallye.rallyesoft.net.Server;
import de.stadtrallye.rallyesoft.threading.Threading;
import de.stadtrallye.rallyesoft.uimodel.ITasksMapControl;
import de.stadtrallye.rallyesoft.uimodel.RallyeTabManager;
import de.stadtrallye.rallyesoft.uimodel.TabManager;

import static de.stadtrallye.rallyesoft.model.structures.LatLngAdapter.toGms;
import static de.stadtrallye.rallyesoft.uimodel.TabManager.getTabManager;

/**
 * Map of all location specific taskManager, combined with a list for direct selection
 * Possibly ability to maximize the map to fullsize
 */
public class TasksMapFragment extends SupportMapFragment implements GoogleMap.OnMarkerClickListener, ITaskManager.ITasksListener, GoogleMap.OnInfoWindowClickListener,
																		ITasksMapControl, IMapManager.IMapListener {

	public static final String TAG = "taskMap";

	private GoogleMap gmap;

	private ITaskManager taskManager;
	private final HashMap<Marker, Integer> markers = new HashMap<>();
	private boolean singleMode;
	private boolean lateInitialization;
	private TabManager tabManager;
	private boolean isLayouted;
	private IMapManager mapManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		lateInitialization = getArguments().getBoolean(Std.LATE_INITIALIZATION, false);
		if (lateInitialization) {
			boolean lateInitSaved = (savedInstanceState != null)? savedInstanceState.getBoolean(Std.LATE_INITIALIZATION, true) : true;
			lateInitialization &= lateInitSaved;
		}


		final Server server = Server.getCurrentServer();
		taskManager = server.acquireTaskManager(this);
		mapManager = server.acquireMapManager(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		isLayouted = false;// If in currently in layout, GoogleMap cannot animateCamera(), have to post it
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
			tabManager = getTabManager(getActivity());
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement ITabActivity");
		}
	}

	@Override
	public void onStart() {
		super.onStart();


		if (gmap == null) {
			gmap = getMap();
			gmap.setMyLocationEnabled(true);
			gmap.setBuildingsEnabled(true);
//			gmap.setOnMarkerClickListener(this);
//			gmap.setOnMapClickListener(this);
//			gmap.setOnCameraChangeListener(this);
			gmap.setOnInfoWindowClickListener(this);
		}

		singleMode = getArguments().getBoolean(Std.TASK_MAP_MODE_SINGLE, false);

//		if (taskManager.getMapLocation() != null)
//			gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(toGms(taskManager.getMapLocation()), taskManager.getZoomLevel()));

		if (!singleMode) {
			populateMap();
			taskManager.addListener(this);
		}

		mapManager.addListener(this);

		if (lateInitialization) {
			MapConfig mapConfig = mapManager.getMapConfigCached();
			if (mapConfig != null) {
				animateToConfigBounds(mapConfig);
			} else {
				mapManager.provideMapConfig();
			}
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		taskManager.removeListener(this);
		mapManager.removeListener(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(Std.LATE_INITIALIZATION, lateInitialization);
	}

	private void populateMap() {

		markers.clear();
		gmap.clear();

		Task t;
		Cursor cursor = taskManager.getTasksCursor();
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

	private void animateToConfigBounds(MapConfig mapConfig) {
		final int padding = getResources().getDimensionPixelOffset(R.dimen.map_center_padding);
		LatLngBounds bounds = new LatLngBounds(toGms(mapConfig.bounds.get(0)), toGms(mapConfig.bounds.get(1)));//TODO Only save the SW + NE Corners as used by LatLngBounds
		gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
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
			MapConfig mapConfig = mapManager.getMapConfig();
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

	@Override
	public void onDestroy() {
		super.onDestroy();

		Server.getCurrentServer().releaseTaskManager(this);
	}

	@Override
	public void onMapChange(List<Node> nodes, List<? extends Edge> edges) {

	}

	@Override
	public void onMapConfigChange(MapConfig mapConfig) {
		animateToConfigBounds(mapConfig);
		lateInitialization = false;
	}

	@Override
	public Handler getCallbackHandler() {
		return Threading.getUiExecutor();
	}
}