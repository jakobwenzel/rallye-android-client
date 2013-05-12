package de.stadtrallye.rallyesoft.model;

import java.util.List;
import java.util.Map;

import de.stadtrallye.rallyesoft.model.structures.Edge;
import de.stadtrallye.rallyesoft.model.structures.Node;

public interface IMapListener {

	public void mapUpdate(Map<Integer, Node> nodes, List<Edge> edges);
}
