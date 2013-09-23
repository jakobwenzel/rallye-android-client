package de.stadtrallye.rallyesoft.fragments;


import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.MapConfig;
import de.rallye.model.structures.Node;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.SherlockMapFragment;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IMap;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.ITabActivity;

import static de.stadtrallye.rallyesoft.model.Model.getModel;
import static de.stadtrallye.rallyesoft.model.structures.LatLngAdapter.toGms;

public class GameMapFragment extends SherlockMapFragment implements IMap.IMapListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnCameraChangeListener {
	
	private static final String THIS = GameMapFragment.class.getSimpleName();
	
	private enum Zoom { ToGame, ToBounds, ZoomCustom }
	
	private GoogleMap gmap;
	private IMap map;
	private final HashMap<Marker, Node> markers = new HashMap<Marker, Node>();

	private LatLngBounds gameBounds;
	private LatLngBounds currentBounds;
	private Zoom zoom;

	private MenuItem centerMenuItem;
	private MenuItem refreshMenuItem;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		if (savedInstanceState != null) {
			zoom = (Zoom) savedInstanceState.getSerializable(Std.ZOOM);
			
			if (zoom == Zoom.ToBounds) {
				try {
					currentBounds = savedInstanceState.getParcelable(Std.MAP_BOUNDS);
				} catch (Exception e) {
					Log.e(THIS, "No Parcel for currentBounds");
				}
			}
		}
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if (gmap == null) {
			gmap = getMap();
			gmap.setMyLocationEnabled(true);
			gmap.setOnMarkerClickListener(this);
			gmap.setOnMapClickListener(this);
			gmap.setOnCameraChangeListener(this);
		}

		IModel model = getModel(getActivity());
		map = model.getMap();
		
		if (zoom == null) {
			map.provideMap();
		}
		
		map.addListener(this);
	}
	
	
	@Override
	public void onStop() {
		super.onStop();
		
		map.removeListener(this);
		map = null;
	}
	
	private void zoomMap() {
		if (zoom == null) {
			Log.e(THIS, "zoom is null");
			return;
		}
		
		final int padding = getResources().getDimensionPixelOffset(R.dimen.map_center_padding);
		
		switch (zoom) {
		case ToBounds:
			gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(currentBounds, padding));
			break;
		case ToGame:
			gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(gameBounds, padding));
			break;
		case ZoomCustom:
			break;
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		centerMenuItem = menu.add(Menu.NONE, R.id.center_menu, Menu.NONE, R.string.center);
		centerMenuItem.setIcon(R.drawable.ic_center_light);
		centerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		refreshMenuItem = menu.add(Menu.NONE, R.id.refresh_menu, Menu.NONE, R.string.refresh);
		refreshMenuItem.setIcon(R.drawable.ic_refresh_light);
		refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean drawerOpen = ((ITabActivity) getActivity()).getTabManager().isMenuOpen();

		refreshMenuItem.setVisible(!drawerOpen);
		centerMenuItem.setVisible(!drawerOpen);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh_menu:
			map.refresh();
			return true;
		case R.id.center_menu:
			if (gameBounds != null) {
				zoom = Zoom.ToGame;
				zoomMap();
			} else {
				Toast.makeText(getActivity(), getString(R.string.no_nodes), Toast.LENGTH_SHORT).show();
			}
			return true;
		default:
			Log.d(THIS, "No hit on menu item "+ item);
			return false;
		}
	}
	
	private static int getColor(Edge.Type t) {
		switch (t) {
		case Bike:
			return 0xbb00ff00;
		case Bus:
			return 0xbb0000bb;
		case Foot:
			return 0xbb555555;
		case Tram:
			return 0xbb000000;
		default:
			return 0xff000000;
		}
	}


	@Override
	public void mapUpdate(Map<Integer, ? extends Node> nodes, List<? extends Edge> edges) {
		
		Builder bounds = LatLngBounds.builder();
		boolean hasBounds = false;
		
		for (Node n: nodes.values()) {
			Marker m = gmap.addMarker(new MarkerOptions()
								.position(toGms(n.location))
								.title(n.name)
								.snippet(n.description));
			
			hasBounds = true;
			bounds.include(toGms(n.location));
			markers.put(m, n);
		}
		
		
		for (Edge e: edges) {
			gmap.addPolyline(new PolylineOptions()
							.add(toGms(e.nodeA.location), toGms(e.nodeB.location))
							.color(getColor(e.type)));
		}
		
		if (hasBounds) {
			gameBounds = bounds.build();
			
			if (zoom == null) {
				zoom = Zoom.ToGame;
			}
			
			zoomMap();
		}
	}

	@Override
	public void onMapConfigChange(MapConfig mapConfig) {
		gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(toGms(mapConfig.location), mapConfig.zoomLevel));
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		
		Node source = markers.get(marker),
				target;
		Builder bounds = LatLngBounds.builder();
		ArrayList<Node> targets = new ArrayList<Node>();
		
		bounds.include(toGms(source.location));
		
		for (Edge e: source.getEdges()) {
			target = e.getOtherNode(source);
			targets.add(target);
			bounds.include(toGms(target.location));
		}
		Log.i(THIS, "found "+ targets.size() +" targets/edges");
		
		for (Entry<Marker, Node> m: markers.entrySet()) {
			Marker current = m.getKey();
			boolean cond = targets.contains(m.getValue());
			current.setVisible(cond);
		}
		marker.setVisible(true);
		
		zoom = Zoom.ToBounds;
		currentBounds = bounds.build();
		zoomMap();
		marker.showInfoWindow();
		
		return true;
	}
	
	@Override
	public void onCameraChange(CameraPosition arg0) {
		
	}

	@Override
	public void onMapClick(LatLng point) {
		
		for (Marker m: markers.keySet()) {
			m.setVisible(true);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable(Std.ZOOM, zoom);
		outState.putParcelable(Std.MAP_BOUNDS, currentBounds);
	}
}
