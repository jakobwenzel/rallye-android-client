package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.structures.MapNode;

public interface IMapListener {

	public void nodeUpdate(List<MapNode> nodes);
}
