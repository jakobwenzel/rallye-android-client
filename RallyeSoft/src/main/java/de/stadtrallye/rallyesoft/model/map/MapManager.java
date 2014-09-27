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
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.model.map;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.Map;
import de.rallye.model.structures.MapConfig;
import de.rallye.model.structures.Node;
import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.tasks.TaskManager;
import de.stadtrallye.rallyesoft.net.Server;
import de.stadtrallye.rallyesoft.net.retrofit.RetroAuthCommunicator;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Edges;
import de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.Nodes;
import de.stadtrallye.rallyesoft.util.converters.Serialization;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_EDGES;
import static de.stadtrallye.rallyesoft.storage.db.DatabaseHelper.EDIT_NODES;

public class MapManager implements IMapManager {
	
	private static final String THIS = TaskManager.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);

	private MapConfig mapConfig;
	private ReadWriteLock configLock = new ReentrantReadWriteLock();
	private boolean refreshingMap = false;
	private boolean refreshingConfig = false;

	private final SQLiteDatabase db;
	private final RetroAuthCommunicator comm;
	
	private final List<IMapListener> mapListeners = new ArrayList<>();


	public MapManager(RetroAuthCommunicator comm, SQLiteDatabase db) {
		this.db = db;
		this.comm = comm;

		if (Storage.hasStructureChanged(EDIT_EDGES | EDIT_NODES)) {
			forceRefresh();
			Storage.structureChangeHandled(EDIT_EDGES | EDIT_NODES);
		}

        //TODO async preload MapConfig...
	}

	public MapManager() throws NoServerKnownException {
		this(Server.getCurrentServer().getAuthCommunicator(), Storage.getDatabase());
	}
	
	@Override
	public void updateMap() throws NoServerKnownException {
		checkServerKnown();

		synchronized (this) {
			if (refreshingMap) {
				Log.w(THIS, "Preventing concurrent Map refreshes");
				return;
			}
			refreshingMap = true;
		}

		comm.getMap(new Callback<Map>() {
			@Override
			public void success(Map map, Response response) {
				updateDatabase(map.nodes, map.edges);
				notifyMapUpdate(map.nodes, map.edges);
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Update failed", e);
				//TODO Server.getServer().commFailed(e);
			}
		});//TODO combine Nodes + Edges into 1 single REST Call, since they depend on each other and are very unlikely to change independently
	}

	private void checkServerKnown() throws NoServerKnownException {
		if (comm == null)
			throw new NoServerKnownException();
	}

	@Override
	public void forceRefresh() {

	}

	@Override
	public void provideMap() {
		List<Node> nodes = new ArrayList<>();
		ArrayList<Edge> edges = new ArrayList<>();
		readDatabase(nodes, edges);
		notifyMapUpdate(nodes, edges);
	}
	
	
	private void updateDatabase(List<Node> nodes, List<Edge> edges) {

		db.beginTransaction();
		try {
			db.delete(Edges.TABLE, null, null);
			db.delete(Nodes.TABLE, null, null);

			SQLiteStatement nodeIn = db.compileStatement("INSERT INTO "+ Nodes.TABLE +
					" ("+ DatabaseHelper.strStr(Nodes.COLS) +") VALUES (?, ?, ?, ?, ?)");

			SQLiteStatement edgeIn = db.compileStatement("INSERT INTO "+ Edges.TABLE +
					" ("+ DatabaseHelper.strStr(Edges.COLS) +") VALUES (?, ?, ?)");

			for (Node n: nodes) {
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
	
	private void readDatabase(List<Node> nodes, List<Edge> edges) {
		Cursor c = db.query(Nodes.TABLE, Nodes.COLS, null, null, null, null, null);

		while (c.moveToNext()) {
			nodes.add(new Node((int) c.getLong(0), c.getString(1), new LatLng(c.getDouble(2), c.getDouble(3)), c.getString(4)));
		}
		c.close();

		c = db.query(Edges.TABLE, Edges.COLS, null, null, null, null, null);

		while (c.moveToNext()) {
			edges.add(new Edge(nodes.get((int) c.getLong(0)), nodes.get((int) c.getLong(1)), c.getString(2)));
		}
		c.close();
	}
	
//	private void findNeighbors(Node node) {
//		Cursor c = server.db.query(Edges.TABLE +" LEFT JOIN "+ Nodes.TABLE +" ON "+ Edges.KEY_B+"="+Nodes.KEY_ID,
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

	private void notifyMapUpdate(final List<Node> nodes, final List<Edge> edges) {
		Handler handler;
		for (final IMapListener l: mapListeners) {
			handler = l.getCallbackHandler();
			if (handler == null) {
				l.onMapChange(nodes, edges);
			} else {
				handler.post(new Runnable() {
					@Override
					public void run() {
						l.onMapChange(nodes, edges);
					}
				});
			}
		}
	}

    @Override
	public void updateMapConfig() throws NoServerKnownException {
        checkServerKnown();

		synchronized (this) {
			if (refreshingConfig) {
				Log.w(THIS, "Preventing concurrent Config refreshes");
				return;
			}
			refreshingConfig = true;
		}


		comm.getMapConfig(new Callback<MapConfig>() {
			@Override
			public void success(MapConfig mapConfig, Response response) {
				if (!mapConfig.equals(MapManager.this.mapConfig)) {
					MapManager.this.mapConfig = mapConfig;
					saveMapConfig();
					Log.d(THIS, "Map Config has changed, replacing");
					notifyMapConfigChange();
				}
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "MapConfig Update failed", e);
				//TODO Server.getServer().commFailed(e);
			}
		});
    }

	private void saveMapConfig() {
		ObjectMapper mapper = Serialization.getInstance();
		try {
			mapper.writeValue(Storage.getMapConfigOutputStream(), mapConfig);
		} catch (IOException e) {
			Log.e(THIS, "Failed to save MapConfig", e);
		}
	}

	private void loadMapConfig() {
		ObjectMapper mapper = Serialization.getInstance();
		try {
			mapper.readValue(Storage.getMapConfigInputStream(), MapConfig.class);
		} catch (FileNotFoundException e) {
			Log.w(THIS, "No previously saved MapConfig found");
		} catch (IOException e) {
			Log.e(THIS, "Failed to load MapConfig", e);
		}
	}

	@Override
	public MapConfig getMapConfig() {
		return mapConfig;//TODO either delete or make blocking UNTIL mapConfig is available (i figure we do not need that anymore)
	}

	private void notifyMapConfigChange() {
		Handler handler;
		for (final IMapListener l: mapListeners) {
			handler = l.getCallbackHandler();
			if (handler == null) {
				l.onMapConfigChange(MapManager.this.mapConfig);
			} else {
				handler.post(new Runnable() {
					@Override
					public void run() {
						l.onMapConfigChange(MapManager.this.mapConfig);
					}
				});
			}
		}
	}

	@Override
	public MapConfig getMapConfigCached() {
        return mapConfig;
    }

	@Override
	public void provideMapConfig() {

	}
}
