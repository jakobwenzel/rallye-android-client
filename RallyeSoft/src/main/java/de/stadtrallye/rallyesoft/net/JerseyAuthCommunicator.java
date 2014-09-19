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

package de.stadtrallye.rallyesoft.net;

import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import de.rallye.model.structures.Chatroom;
import de.rallye.model.structures.Edge;
import de.rallye.model.structures.MapConfig;
import de.rallye.model.structures.Node;
import de.rallye.model.structures.SimpleChatEntry;
import de.rallye.model.structures.SimpleSubmission;
import de.rallye.model.structures.Submission;
import de.rallye.model.structures.User;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.model.structures.Task;

/**
* Created by Ramon on 18.09.2014.
*/
public class JerseyAuthCommunicator {

	private final ServerLogin login;
	private final WebTarget server;

	public JerseyAuthCommunicator(ServerLogin login, WebTarget server) {
		this.login = login;
		this.server = server;
	}

	private String getLogoutGroup() {
		return Integer.toString(login.getUserID()); // "any" instead?
	}

	public Future<String> logout(InvocationCallback<String> callback) {
		return server
				.path(Paths.GROUPS).path(getLogoutGroup()).path(Integer.toString(login.getUserID()))
				.request()
				.async()
				.delete(callback);
	}

	public Future<List<Chatroom>> getAvailableChatrooms(InvocationCallback<List<Chatroom>> callback) {
		return server
				.path(Paths.CHATROOMS)
				.request().async()
				.get(callback);
	}

	private String getChatPath(int chatroom, Long since) {
		StringBuilder sb = new StringBuilder();
		sb.append(chatroom).append('/');
		if (since != null)
			sb.append(Paths.SNIPPET_SINCE).append(since);

		return sb.toString();
	}

	public Future<List<ChatEntry>> getNewChats(int chatroom, Long since, InvocationCallback<List<ChatEntry>> callback) {
		return server
				.path(Paths.CHATROOMS).path(getChatPath(chatroom, since))
				.request().async()
				.get(callback);
	}

	public Future<List<Node>> getMapNodes(InvocationCallback<List<Node>> callback) {
		return server
				.path(Paths.MAP_NODES)
				.request().async()
				.get(callback);
	}

	public Future<List<Edge>> getMapEdges(InvocationCallback<List<Edge>> callback) {
		return server
				.path(Paths.MAP_EDGES)
				.request().async()
				.get(callback);
	}

	public Future<ChatEntry> postChatMessage(int chatroom, SimpleChatEntry message, InvocationCallback<ChatEntry> callback) {
		return server
				.path(Paths.CHATROOMS).path(Integer.toString(chatroom))
				.request().async()
				.put(Entity.entity(message, MediaType.APPLICATION_JSON_TYPE), callback);
	}

	public Future<MapConfig> getMapConfig(InvocationCallback<MapConfig> callback) {
		return server
				.path(Paths.MAP_CONFIG)
				.request().async()
				.get(callback);
	}

	public Future<List<User>> getAllUsers(InvocationCallback<List<User>> callback) {
		return server
				.path(Paths.USERS)
				.request().async()
				.get(callback);
	}

	public Future<List<Task>> getTasks(InvocationCallback<List<Task>> callback) {
		return server
				.path(Paths.TASKS)
				.request().async()
				.get(callback);
	}

	public Future<List<Submission>> getSubmissionsForGroup(InvocationCallback<List<Submission>> callback) {
		return server
				.path(Paths.TASKS_SUBMISSIONS_ALL).path(Integer.toString(login.getGroupID()))
				.request().async()
				.get(callback);
	}

	public Future<Submission> submitSolution(int taskId, SimpleSubmission submission, InvocationCallback<Submission> callback) {
		return server
				.path(Paths.TASKS).path(Integer.toString(taskId))
				.request().async()
				.put(Entity.entity(submission, MediaType.APPLICATION_JSON_TYPE), callback);
	}

}
