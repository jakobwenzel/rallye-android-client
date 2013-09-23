package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.MapConfig;
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
	 * Get the center of the game on the map as well as the initial zoomLevel (a Google Map can only be initialized with 1 set of coordinates at this time)
	 * @return MapConfig
	 */
	MapConfig getMapConfig();

	/**
	 * Request a callback to all announced Listeners: IMapListener.mapUpdate()
	 */
	void provideMap();

	/**
	 * contains a callback for when the map content has changed
	 */
	public interface IMapListener {
		public void mapUpdate(java.util.Map<Integer, ? extends Node> nodes, List<? extends Edge> edges);

        void onMapConfigChange(MapConfig mapConfig);
    }
}
