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
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.model.map;

import java.util.List;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.MapConfig;
import de.rallye.model.structures.Node;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.IHandlerCallback;

/**
 * Model representation of a Street Map (for Scotland-Yard-type games)
 * Consists of Nodes with Name and Description,
 * and Edges of different types, connecting the Nodes
 * TODO: Players can "move" along Edges and assigned to a Node
 */
public interface IMapManager {
	/**
	 * update from server
	 */
	void updateMap() throws NoServerKnownException;

	/**
	 * force refresh
	 */
	void forceRefreshMapConfig();

	void addListener(IMapListener l);
	void removeListener(IMapListener l);

	void updateMapConfig() throws NoServerKnownException;

	/**
	 * Get the center of the game on the map as well as the initial zoomLevel (a Google Map can only be initialized with 1 set of coordinates at this time)
	 * @return MapConfig if cached, null otherwise
	 */
	MapConfig getMapConfigCached();

	/**
	 * Request a callback to all announced Listeners: IMapListener.onMapChange()
	 */
	void provideMap();



	void provideMapConfig();

	/**
	 * contains a callback for when the map content has changed
	 */
	public interface IMapListener extends IHandlerCallback {
		void onMapChange(List<Node> nodes, List<? extends Edge> edges);

        void onMapConfigChange(MapConfig mapConfig);
    }
}
