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

package de.stadtrallye.rallyesoft.model.converters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import de.rallye.model.structures.AdditionalPicture;
import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.Edge;
import de.rallye.model.structures.Group;
import de.rallye.model.structures.GroupUser;
import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.MapConfig;
import de.rallye.model.structures.Node;
import de.rallye.model.structures.ServerInfo;
import de.rallye.model.structures.Submission;
import de.rallye.model.structures.TaskSubmissions;
import de.rallye.model.structures.User;
import de.stadtrallye.rallyesoft.model.Chatroom;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.model.structures.Task;
import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONArrayConverter;
import de.stadtrallye.rallyesoft.util.JSONConverter;

/**
 * Collection of JSONConverters to create all structures from JSON
 * Supplement for Jackson on server side (Library would take over 1 mb)
 */
@Deprecated
public abstract class JsonConverters {

	public static class ChatConverter extends JSONConverter<ChatEntry> {

		@Override
		public ChatEntry doConvert(JSONObject o) throws JSONException {
			return new ChatEntry(o.getInt(ChatEntry.CHAT_ID),
					o.getString(ChatEntry.MESSAGE),
					o.getInt(ChatEntry.TIMESTAMP),
					o.getInt(ChatEntry.GROUP_ID),
					o.getInt(ChatEntry.USER_ID),
					o.optInt(ChatEntry.PICTURE_ID));
		}
	}

	public static class ChatroomConverter extends JSONConverter<Chatroom> {

		private final Model model;

		public ChatroomConverter(Model model) {
			this.model = model;
		}

		@Override
		public Chatroom doConvert(JSONObject o) throws JSONException {
			int i = o.getInt(de.rallye.model.structures.Chatroom.CHATROOM_ID);
			String name = o.getString(de.rallye.model.structures.Chatroom.NAME);

			return new Chatroom(i, name, model);
		}
	}

	public static class GroupConverter extends JSONConverter<Group> {

		@Override
		public Group doConvert(JSONObject o) throws JSONException {
			return new Group(o.getInt(Group.GROUP_ID),
					o.getString(Group.NAME),
					o.getString(Group.DESCRIPTION));
		}
	}

	public static class TaskConverter extends JSONConverter<Task> {

		private final JSONArrayConverter<AdditionalResource> converter = new JSONArrayConverter<AdditionalResource>(new AdditionalResourceConverter());

		@Override
		public Task doConvert(JSONObject o) throws JSONException {
			LatLng coords;
			if (o.isNull(Task.LOCATION))
				coords = null;
			else {
				JSONObject p = o.getJSONObject(Task.LOCATION);
				coords = new LatLng(p.getDouble(LatLng.LAT), p.getDouble(LatLng.LNG));
			}

			List<AdditionalResource> res = converter.convert(o.getJSONArray(Task.ADDITIONAL_RESOURCES));

			return new Task(o.getInt(Task.TASK_ID), o.getBoolean(Task.LOCATION_SPECIFIC),
					coords, o.getDouble(Task.RADIUS), o.getString(Task.NAME), o.getString(Task.DESCRIPTION),
					o.getBoolean(Task.MULTIPLE_SUBMITS), o.getInt(Task.SUBMIT_TYPE), o.getString(Task.POINTS),
					res, Task.SUBMITS_UNKNOWN);
		}
	}

	public static class TaskSubmissionsIndexer implements IConverter<TaskSubmissions, Integer> {
		@Override
		public Integer convert(TaskSubmissions input) {
			return input.taskID;
		}
	}

	public static class TaskSubmissionsCompressor implements IConverter<TaskSubmissions, List<Submission>> {
		@Override
		public List<Submission> convert(TaskSubmissions input) {
			return input.submissions;
		}
	}

	public static class TaskSubmissionsConverter extends JSONConverter<TaskSubmissions> {

		private final JSONArrayConverter<Submission> converter = new JSONArrayConverter<Submission>(new SubmissionConverter());

		@Override
		public TaskSubmissions doConvert(JSONObject o) throws JSONException {
			List<Submission> res = converter.convert(o.getJSONArray(TaskSubmissions.SUBMISSIONS));

			Integer score = o.isNull(TaskSubmissions.SCORE)?null:o.getInt(TaskSubmissions.SCORE);
			Integer bonus = o.isNull(TaskSubmissions.BONUS)?null:o.getInt(TaskSubmissions.BONUS);
			return new TaskSubmissions(o.getInt(TaskSubmissions.TASK_ID), o.getInt(TaskSubmissions.GROUP_ID), res, score, bonus, o.getBoolean(TaskSubmissions.SCORE_OUTDATED));
		}
	}

	public static class SubmissionConverter extends JSONConverter<Submission> {

		@Override
		public Submission doConvert(JSONObject o) throws JSONException {
			Integer intSubmission = (o.isNull(Submission.INT_SUBMISSION))? null : o.getInt(Submission.INT_SUBMISSION);

			return new Submission(o.getInt(Submission.SUBMISSION_ID), o.getInt(Submission.SUBMIT_TYPE),
					intSubmission, o.getString(Submission.TEXT_SUBMISSION));
		}
	}

	public static class AdditionalResourceConverter extends JSONConverter<AdditionalResource> {

		@Override
		public AdditionalResource doConvert(JSONObject o) throws JSONException {
			if (o.has(AdditionalPicture.PICTURE_ID)) {
				return new AdditionalPicture(o.getInt(AdditionalPicture.PICTURE_ID));
			} else {
				return null;
			}
		}
	}

	public static class EdgeConverter extends JSONConverter<Edge> {

		private final Map<Integer, Node> nodes;

		public EdgeConverter(Map<Integer, Node> nodes) {
			this.nodes = nodes;
		}

		@Override
		public Edge doConvert(JSONObject o) throws JSONException {
			return new Edge(
					nodes.get(o.getInt(Edge.NODE_A)),
					nodes.get(o.getInt(Edge.NODE_B)),
					o.getString(Edge.TYPE));
		}

	}

	public static class NodeConverter extends JSONConverter<Node> {
		private final JSONConverter<LatLng> converter = new LatLngConverter();

		@Override
		public Node doConvert(JSONObject o) throws JSONException {

			return new Node(
					o.getInt(Node.NODE_ID),
					o.getString(Node.NAME),
					converter.convert(o.getJSONObject(Node.LOCATION)),
					o.getString(Node.DESCRIPTION));
		}
	}

	public static class NodeIndexer implements IConverter<Node, Integer> {
		@Override
		public Integer convert(Node input) {
			return input.nodeID;
		}
	}

	public static class LatLngConverter extends JSONConverter<LatLng> {

		@Override
		public LatLng doConvert(JSONObject o) throws JSONException {
			return new LatLng(o.getDouble(LatLng.LAT), o.getDouble(LatLng.LNG));
		}
	}

	public static class MapConfigConverter extends JSONConverter<MapConfig> {
		private final JSONConverter<LatLng> converter = new LatLngConverter();
		private final JSONArrayConverter<LatLng> arrayConverter = new JSONArrayConverter<LatLng>(converter);

		@Override
		public MapConfig doConvert(JSONObject o) throws JSONException {

			return new MapConfig(o.getString(MapConfig.NAME),
					converter.convert(o.getJSONObject(MapConfig.LOCATION)),
                    (float) o.getDouble(MapConfig.ZOOM_LEVEL),
					arrayConverter.convert(o.getJSONArray(MapConfig.BOUNDS)));
		}
	}

	public static class ServerInfoConverter extends JSONConverter<ServerInfo> {

		private final JSONArrayConverter<ServerInfo.Api> converter = new JSONArrayConverter<ServerInfo.Api>(new ServerInfoApiConverter());

		@Override
		public ServerInfo doConvert(JSONObject o) throws JSONException {
			JSONArray js = o.getJSONArray(ServerInfo.API);

//			ServerInfo.Api[] apis = new ServerInfo.Api[js.length()];
//
//			for (int i=0; i<js.length(); i++) {
//				apis[i] = converter.doConvert(js.getJSONObject(i));
//			}

			return new ServerInfo(o.getString(ServerInfo.NAME), o.getString(ServerInfo.DESCRIPTION), converter.convert(js).toArray(new ServerInfo.Api[js.length()]), o.getString(ServerInfo.BUILD));
		}
	}
	public static class ServerInfoApiConverter extends JSONConverter<ServerInfo.Api> {

		@Override
		public ServerInfo.Api doConvert(JSONObject o) throws JSONException {
			return new ServerInfo.Api(o.getString(ServerInfo.Api.NAME), o.getInt(ServerInfo.Api.VERSION));
		}
	}

	public static class GroupUserConverter extends JSONConverter<GroupUser> {
		@Override
		public GroupUser doConvert(JSONObject o) throws JSONException {
			return new GroupUser(o.getInt(GroupUser.USER_ID), o.getInt(GroupUser.GROUP_ID), o.getString(GroupUser.NAME));
		}
	}

	public static class UserIndexer implements IConverter<User,Integer> {
		@Override
		public Integer convert(User input) {
			return input.userID;
		}
	}

	public static class GroupIndexer implements IConverter<Group,Integer> {
		@Override
		public Integer convert(Group input) {
			return input.groupID;
		}
	}
}
