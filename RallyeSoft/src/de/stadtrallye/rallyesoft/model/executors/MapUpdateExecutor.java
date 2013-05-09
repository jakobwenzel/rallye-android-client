package de.stadtrallye.rallyesoft.model.executors;

import java.util.List;
import java.util.Map;

import de.stadtrallye.rallyesoft.model.comm.Request;
import de.stadtrallye.rallyesoft.model.structures.MapEdge;
import de.stadtrallye.rallyesoft.model.structures.MapNode;
import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONArray;

public class MapUpdateExecutor extends MyRunnable<Map<Integer, MapNode>> {
	
	private Map<Integer, MapNode> nodes;
	private List<MapEdge> edges;
	private Callback model;
	private Request edgeRequest;
	private Request nodeRequest;
	
	public MapUpdateExecutor(Request nodeRequest, Request edgeRequest, Callback model) {
		this.model = model;
		this.edgeRequest = edgeRequest;
		this.nodeRequest = nodeRequest;
	}

	@Override
	protected Map<Integer, MapNode> tryRun() throws Exception {

		RequestExecutor<Map<Integer, MapNode>, Void> nodeExecutor = new RequestExecutor<Map<Integer, MapNode>, Void>(nodeRequest, new MapConverter(), null, null);
		nodes = nodeExecutor.tryRun();
		
		RequestExecutor<List<MapEdge>, Void> edgeExecutor = new JSONArrayRequestExecutor<MapEdge, Void>(edgeRequest, new MapEdge.EdgeConverter(nodes), null, null);
		edges = edgeExecutor.tryRun();
		
		return nodes;
	}
	
	public List<MapEdge> getEdges() {
		return edges;
	}
	
	public Map<Integer, MapNode> getNodes() {
		return res;
	}
	
	public interface Callback {
		void mapUpdateResult(MapUpdateExecutor r);
	}

	@Override
	protected void callback() {
		model.mapUpdateResult(this);
	}
	
	public static class MapConverter implements IConverter<String, Map<Integer, MapNode>> {
		
		@Override
		public Map<Integer, MapNode> convert(String input) {
			return JSONArray.getInstance(new MapNode.NodeConverter(), input).toMap(new MapNode.IndexGetter());
		}
	}

}
