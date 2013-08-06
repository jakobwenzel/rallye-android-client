package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.Node;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Edges;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper.Nodes;
import de.stadtrallye.rallyesoft.model.executors.MapUpdateExecutor;

public class Map implements IMap, MapUpdateExecutor.Callback {
	
	private static final String THIS = Tasks.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);
	
	final private Model model;
	
	final ArrayList<IMapListener> mapListeners = new ArrayList<IMapListener>();

	
	Map(Model model) {
		this.model = model;
	}
	
	
	@Override
	public void updateMap() {
		if (!model.isConnectedInternal()) {
			err.notLoggedIn();
			return;
		}
		try {
			model.exec.execute(new MapUpdateExecutor(model.factory.mapNodesRequest(), model.factory.mapEdgesRequest(), this));
		} catch (HttpRequestException e) {
			err.requestException(e);
		}
	}
	
	
	@Override
	public void updateMapResult(MapUpdateExecutor r) {
		if (r.isSuccessful()) {
			try {
				java.util.Map<Integer, Node> nodes = r.getNodes();
				List<Edge> edges = r.getEdges();
				updateDatabase(nodes, edges);
				notifyMapUpdate(nodes, edges);
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
		} else
			err.asyncTaskResponseError(r.getException());
	}
	
	@Override
	public void provideMap() {
		HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();
		ArrayList<Edge> edges = new ArrayList<Edge>();
		readDatabase(nodes, edges);
		notifyMapUpdate(nodes, edges);
	}
	
	
	private void updateDatabase(java.util.Map<Integer, Node> nodes, List<Edge> edges) {
		SQLiteDatabase db = model.db;
		
		db.beginTransaction();
		try {
			db.delete(Edges.TABLE, null, null);
			db.delete(Nodes.TABLE, null, null);
			
			SQLiteStatement nodeIn = db.compileStatement("INSERT INTO "+ Nodes.TABLE +
					" ("+ DatabaseHelper.strStr(Nodes.COLS) +") VALUES (?, ?, ?, ?, ?)");
			
			SQLiteStatement edgeIn = db.compileStatement("INSERT INTO "+ Edges.TABLE +
					" ("+ DatabaseHelper.strStr(Edges.COLS) +") VALUES (?, ?, ?)");
			
			for (Node n: nodes.values()) {
				nodeIn.bindLong(1, n.nodeID);
				nodeIn.bindString(2, n.name);
				nodeIn.bindDouble(3, n.location.latitude);
				nodeIn.bindDouble(4, n.location.longitude);
				nodeIn.bindString(5, n.description);
				nodeIn.executeInsert();
			}
			
			for (Edge m: edges) {
				edgeIn.bindLong(1, m.nodeA.nodeID);
				edgeIn.bindLong(2, m.nodeB.nodeID);
				edgeIn.bindString(3, m.type.toString());
				edgeIn.executeInsert();
			}
			
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e(THIS, "Map Update on Database failed", e);
		} finally {
			db.endTransaction();
		}
	}
	
	private void readDatabase(java.util.Map<Integer, Node> nodes, List<Edge> edges) {
		Cursor c = model.db.query(Nodes.TABLE, Nodes.COLS, null, null, null, null, null);
		
		while (c.moveToNext()) {
			nodes.put((int) c.getLong(0), new Node((int) c.getLong(0), c.getString(1), c.getDouble(2), c.getDouble(3), c.getString(4)));
		}
		c.close();
		
		c = model.db.query(Edges.TABLE, Edges.COLS, null, null, null, null, null);
		
		while (c.moveToNext()) {
			edges.add(new Edge(nodes.get((int) c.getLong(0)), nodes.get((int) c.getLong(1)), c.getString(2)));
		}
		c.close();
	}
	
//	private void findNeighbors(Node node) {
//		Cursor c = model.db.query(Edges.TABLE +" LEFT JOIN "+ Nodes.TABLE +" ON "+ Edges.KEY_B+"="+Nodes.KEY_ID,
//				new String[]{ Edges.KEY_A, Edges.KEY_B, Edges.KEY_TYPE }, Edges.KEY_A+"="+node.ID, null, null, null, null);
//	}

	@Override
	public void addListener(IMapListener l) {
		mapListeners.add(l);
	}
	
	@Override
	public void removeListener(IMapListener l) {
		mapListeners.remove(l);
	}

	private void notifyMapUpdate(final java.util.Map<Integer, Node> nodes, final List<Edge> edges) {
		model.uiHandler.post(new Runnable(){
			@Override
			public void run() {
				for(IMapListener l: mapListeners) {
					l.mapUpdate(nodes, edges);
				}
			}
		});
	}
	
	@Override
	public LatLng getMapLocation() {
		return (model.serverConfig != null)? model.serverConfig.location : null;
	}
	
	@Override
	public float getZoomLevel() {
		return (model.serverConfig != null)? model.serverConfig.zoomLevel : 0;
	}

}
