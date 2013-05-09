package de.stadtrallye.rallyesoft.fragments;

import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import de.stadtrallye.rallyesoft.model.IMapListener;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.MapEdge;
import de.stadtrallye.rallyesoft.model.structures.MapNode;

public class GameMapFragment extends SupportMapFragment implements IMapListener {
	
	private GoogleMap map;
	private Model model;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		

	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		map = getMap();
		map.setMyLocationEnabled(true);
		
		model = Model.getInstance(getActivity().getApplicationContext(), true);
		model.addListener(this);
		model.getMapNodes();
		
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		model.removeListener(this);
	}
	
	public static int getColor(MapEdge.Type t) {
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
	public void mapUpdate(Map<Integer, MapNode> nodes, List<MapEdge> edges) {
		
		for (MapNode n: nodes.values()) {
			map.addMarker(new MarkerOptions()
							.position(n.position)
							.title(n.name)
							.snippet(n.description));
		}
		
		for (MapEdge e: edges) {
			map.addPolyline(new PolylineOptions()
							.add(e.a.position, e.b.position)
							.color(getColor(e.type)));
		}
		
		
		Toast.makeText(getActivity(), nodes.size()+" Nodes loaded!", Toast.LENGTH_SHORT).show();
	}
}
