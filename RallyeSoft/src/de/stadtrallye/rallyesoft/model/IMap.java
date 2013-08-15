package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.Node;

/**
 * Model representation of a Street Map (for Scotland-Yard-type games)
 * Consists of Nodes with Name and Description,
 * and Edges of different types, connecting the Nodes
 * TODO: Players can "move" along Edges and assigned to a Node
 */
public interface IMap {
	/**
	 * Force refresh from server
	 */
	void refresh();

	void addListener(IMapListener l);
	void removeListener(IMapListener l);

	/**
	 * Get the center of the game on the map (a Google Map can only be initialized with 1 set of coordinates at this time)
	 * @return Custom LatLng, modeled after Google Play Services LatLng, because it is closed source and needed on the server side (convert with LatLngAdapter.toGms())
	 */
	LatLng getMapLocation();

	/**
	 * Get a zoom level applicable to the coordinates from getMapLocation()
	 * @return Google Play Services / Google Map compatible
	 */
	float getZoomLevel();

	/**
	 * Request a callback to all announced Listeners: IMapListener.mapUpdate()
	 */
	void provideMap();

	/**
	 * contains a callback for when the map content has changed
	 */
	public interface IMapListener {
		public void mapUpdate(java.util.Map<Integer, ? extends Node> nodes, List<? extends Edge> edges);
	}
}
