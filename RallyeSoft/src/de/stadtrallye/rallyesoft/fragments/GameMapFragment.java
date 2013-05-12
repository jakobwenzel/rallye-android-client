package de.stadtrallye.rallyesoft.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.UIComm.IModelActivity;
import de.stadtrallye.rallyesoft.model.IMap;
import de.stadtrallye.rallyesoft.model.IMapListener;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.structures.Edge;
import de.stadtrallye.rallyesoft.model.structures.Node;

public class GameMapFragment extends SherlockMapFragment implements IMapListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {
	
	private static final String THIS = GameMapFragment.class.getSimpleName();
	private static final int PADDING = 100;
	
	private GoogleMap gmap;
	private IModel model;
	private IMap map;
	private MenuItem refreshMenuItem;
	private HashMap<Marker, Node> markers = new HashMap<Marker, Node>();

	private LatLngBounds gameBounds;
	private MenuItem centerMenuItem;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		try {
			model = ((IModelActivity) getActivity()).getModel();
			map = model.getMap();
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
			gmap.setOnMarkerClickListener(this);
			gmap.setOnMapClickListener(this);
		}

		map.addListener(this);
		map.provideMap();

	}
	
	
	@Override
	public void onStop() {
		super.onStop();
		
		map.removeListener(this);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		centerMenuItem = menu.add(Menu.NONE, R.id.center_menu, Menu.NONE, R.string.center);
		centerMenuItem.setIcon(R.drawable.center);
		centerMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		refreshMenuItem = menu.add(Menu.NONE, R.id.refresh_menu, Menu.NONE, R.string.refresh);
		refreshMenuItem.setIcon(R.drawable.refresh);
		refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh_menu:
			map.updateMap();
			return true;
		case R.id.center_menu:
			gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(gameBounds, PADDING));
			return true;
		default:
			Log.d(THIS, "No hit on menu item "+ item);
			return false;
		}
	}
	
	public static int getColor(Edge.Type t) {
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
	public void mapUpdate(Map<Integer, Node> nodes, List<Edge> edges) {
		
		Builder bounds = LatLngBounds.builder();
		
		for (Node n: nodes.values()) {
			Marker m = gmap.addMarker(new MarkerOptions()
								.position(n.position)
								.title(n.name)
								.snippet(n.description));
			
			bounds.include(n.position);
			markers.put(m, n);
		}
		
		gameBounds = bounds.build();
		
		for (Edge e: edges) {
			gmap.addPolyline(new PolylineOptions()
							.add(e.a.position, e.b.position)
							.color(getColor(e.type)));
		}
		
		gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(gameBounds, PADDING));
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		
		Node source = markers.get(marker),
				target;
		Builder bounds = LatLngBounds.builder();
		ArrayList<Node> targets = new ArrayList<Node>();
		
		bounds.include(source.position);
		
		for (Edge e: source.getEdges()) {
			target = e.getOtherNode(source);
			targets.add(target);
			bounds.include(target.position);
		}
		Log.i(THIS, "found "+ targets.size() +" targets/edges");
		
		for (Entry<Marker, Node> m: markers.entrySet()) {
			Marker current = m.getKey();
			boolean cond = targets.contains(m.getValue());
			current.setVisible(cond);
		}
		marker.setVisible(true);
		
		gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), PADDING));
		marker.showInfoWindow();
		
		return true;
	}

	@Override
	public void onMapClick(LatLng point) {
		
		for (Marker m: markers.keySet()) {
			m.setVisible(true);
		}
	}
}
