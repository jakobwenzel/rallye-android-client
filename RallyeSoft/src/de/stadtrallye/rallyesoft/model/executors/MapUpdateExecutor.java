package de.stadtrallye.rallyesoft.model.executors;

import java.util.List;
import java.util.Map;

import de.stadtrallye.rallyesoft.model.structures.Edge;
import de.stadtrallye.rallyesoft.model.structures.Node;
import de.stadtrallye.rallyesoft.net.Request;
import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONArray;

public class MapUpdateExecutor extends MyRunnable<Map<Integer, Node>> {
	
	private Map<Integer, Node> nodes;
	private List<Edge> edges;
	private Callback model;
	private Request edgeRequest;
	private Request nodeRequest;
	
	public MapUpdateExecutor(Request nodeRequest, Request edgeRequest, Callback model) {
		this.model = model;
		this.edgeRequest = edgeRequest;
		this.nodeRequest = nodeRequest;
	}

	@Override
	protected Map<Integer, Node> tryRun() throws Exception {
		
		nodes = new MapConverter().convert(nodeRequest.execute());
		
		edges = edgeRequest.executeJSONArray(new Edge.EdgeConverter(nodes));
		
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
	
	public static class MapConverter implements IConverter<String, Map<Integer, Node>> {
		
		@Override
		public Map<Integer, Node> convert(String input) {
			return JSONArray.getInstance(new Node.NodeConverter(), input).toMap(new Node.IndexGetter());
		}
	}

}
