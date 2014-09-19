/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.net.jersey;

import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.LoginInfo;
import de.rallye.model.structures.ServerInfo;
import de.rallye.model.structures.UserAuth;
import de.stadtrallye.rallyesoft.net.Paths;

/**
 * Created by Ramon on 18.09.2014.
 */
public class JerseyCommunicator {

	private final WebTarget server;

	public JerseyCommunicator(String host) {
		Client client = ClientBuilder.newClient();

		server = client.target(host);
	}

	public Future<ServerInfo> getServerInfo(InvocationCallback<ServerInfo> callback) {
		return server
				.path(Paths.SERVER_INFO)
				.request()
				.async()
				.get(callback);
	}

	public Future<List<Group>> getAvailableGroups(InvocationCallback<List<Group>> callback) {
		return server
				.path(Paths.GROUPS)
				.request()
				.async()
				.get(callback);
	}

	public Future<UserAuth> login(int groupId, LoginInfo login, InvocationCallback<UserAuth> callback) {
		return server
				.path(Paths.GROUPS).path(Integer.toString(groupId))
				.request()
				.async()
				.put(Entity.entity(login, MediaType.APPLICATION_JSON_TYPE), callback);
	}

	/*public Future<ServerStatus> getServerStatus(InvocationCallback<ServerStatus> callback) {
		return server
				.path(Paths.SERVER_STATUS)
				.request()
				.async()
				.get(callback);
	}*/


}
