package de.stadtrallye.rallyesoft.model;

import java.util.List;
import java.util.Map;

import de.stadtrallye.rallyesoft.model.structures.MapEdge;
import de.stadtrallye.rallyesoft.model.structures.MapNode;

public interface IMapListener {

	public void mapUpdate(Map<Integer, MapNode> nodes, List<MapEdge> edges);
}
