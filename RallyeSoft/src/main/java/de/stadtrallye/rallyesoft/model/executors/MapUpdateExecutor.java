package de.stadtrallye.rallyesoft.model.executors;

import java.util.List;
import java.util.Map;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.Node;
import de.stadtrallye.rallyesoft.model.converters.JsonConverters;
import de.stadtrallye.rallyesoft.net.Request;
import de.stadtrallye.rallyesoft.util.JSONArray;

public class MapUpdateExecutor extends MyRunnable<Map<Integer, Node>> {

	private List<Edge> edges;
	private final Callback model;
	private final Request edgeRequest;
	private final Request nodeRequest;
	
	public MapUpdateExecutor(Request nodeRequest, Request edgeRequest, Callback model) {
		this.model = model;
		this.edgeRequest = edgeRequest;
		this.nodeRequest = nodeRequest;
	}

	@Override
	protected Map<Integer, Node> tryRun() throws Exception {

		Map<Integer, Node> nodes = JSONArray.getInstance(new JsonConverters.NodeConverter(), nodeRequest.execute()).toMap(new JsonConverters.NodeIndexer(), null);
		
		edges = edgeRequest.executeJSONArray(new JsonConverters.EdgeConverter(nodes));
		
		return nodes;
	}
	
	public List<Edge> getEdges() {
		return edges;
	}
	
	public Map<Integer, Node> getNodes() {
		return res;
	}
	
	public interface Callback {
		void updateMapResult(MapUpdateExecutor r);
	}

	@Override
	protected void callback() {
		model.updateMapResult(this);
	}
}
