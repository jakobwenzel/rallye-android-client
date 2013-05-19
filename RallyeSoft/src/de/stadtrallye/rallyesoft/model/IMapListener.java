package de.stadtrallye.rallyesoft.model;

import java.util.List;
import java.util.Map;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.Node;

public interface IMapListener {

	public void mapUpdate(Map<Integer, ? extends Node> nodes, List<? extends Edge> edges);
}
