///*
// * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
// *
// * This file is part of RallyeSoft.
// *
// * RallyeSoft is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * Foobar is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
// */
//
//package de.stadtrallye.rallyesoft.util.executors;
//
//import java.util.List;
//import java.util.Map;
//
//import de.rallye.model.structures.Edge;
//import de.rallye.model.structures.Node;
//import de.stadtrallye.rallyesoft.util.converters.JsonConverters;
//import de.stadtrallye.rallyesoft.net.manual.Request;
//import de.stadtrallye.rallyesoft.util.JSONArray;
//
//public class MapUpdateExecutor extends MyRunnable<Map<Integer, Node>> {
//
//	private List<Edge> edges;
//	private final Callback model;
//	private final Request edgeRequest;
//	private final Request nodeRequest;
//
//	public MapUpdateExecutor(Request nodeRequest, Request edgeRequest, Callback model) {
//		this.model = model;
//		this.edgeRequest = edgeRequest;
//		this.nodeRequest = nodeRequest;
//	}
//
//	@Override
//	protected Map<Integer, Node> tryRun() throws Exception {
//
//		Map<Integer, Node> nodes = JSONArray.getInstance(new JsonConverters.NodeConverter(), nodeRequest.execute()).toMap(new JsonConverters.NodeIndexer(), null);
//
//		edges = edgeRequest.executeJSONArray(new JsonConverters.EdgeConverter(nodes));
//
//		return nodes;
//	}
//
//	public List<Edge> getEdges() {
//		return edges;
//	}
//
//	public Map<Integer, Node> getNodes() {
//		return res;
//	}
//
//	public interface Callback {
//		void updateMapResult(MapUpdateExecutor r);
//	}
//
//	@Override
//	protected void callback() {
//		model.updateMapResult(this);
//	}
//}
