package de.stadtrallye.rallyesoft.model.converters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import de.rallye.model.structures.Edge;
import de.rallye.model.structures.Group;
import de.rallye.model.structures.GroupUser;
import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.Node;
import de.rallye.model.structures.ServerConfig;
import de.rallye.model.structures.ServerInfo;
import de.rallye.model.structures.Task;
import de.rallye.model.structures.User;
import de.stadtrallye.rallyesoft.model.Chatroom;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONConverter;

/**
 * Created by Ramon on 25.06.13
 */
public class JsonConverters {

	public static class ChatroomConverter extends JSONConverter<Chatroom> {

		private Model model;

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

		@Override
		public Task doConvert(JSONObject o) throws JSONException {
			LatLng coords;
			if (o.isNull(Task.LOCATION))
				coords = null;
			else {
				JSONObject p = o.getJSONObject(Task.LOCATION);
				coords = new LatLng(p.getDouble(LatLng.LAT), p.getDouble(LatLng.LNG));
			}

			return new Task(o.getInt(Task.TASK_ID), o.getBoolean(Task.LOCATION_SPECIFIC),
					coords, o.getString(Task.NAME), o.getString(Task.DESCRIPTION),
					o.getBoolean(Task.MULTIPLE_SUBMITS), o.getInt(Task.SUBMIT_TYPE));
		}
	}

	public static class EdgeConverter extends JSONConverter<Edge> {

		final private Map<Integer, Node> nodes;

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

		@Override
		public Node doConvert(JSONObject o) throws JSONException {
			JSONObject p = o.getJSONObject(Node.LOCATION);

			return new Node(
					o.getInt(Node.NODE_ID),
					o.getString(Node.NAME),
					p.getDouble(LatLng.LAT),
					p.getDouble(LatLng.LNG),
					o.getString(Node.DESCRIPTION));
		}

	}

	public static class NodeIndexer implements IConverter<Node, Integer> {
		@Override
		public Integer convert(Node input) {
			return input.nodeID;
		}
	}

	public static class ServerConfigConverter extends JSONConverter<ServerConfig> {
		@Override
		public ServerConfig doConvert(JSONObject o) throws JSONException {
			JSONObject p = o.getJSONObject(ServerConfig.LOCATION);

			return new ServerConfig(o.getString(ServerConfig.NAME),
					p.getDouble(LatLng.LAT),
					p.getDouble(LatLng.LNG),
					o.getInt(ServerConfig.ROUNDS),
					o.getInt(ServerConfig.ROUND_TIME),
					o.getLong(ServerConfig.START_TIME));
		}
	}

	public static class ServerInfoConverter extends JSONConverter<ServerInfo> {
		@Override
		public ServerInfo doConvert(JSONObject o) throws JSONException {
			JSONArray js = o.getJSONArray(ServerInfo.API);

			ServerInfo.Api[] apis = new ServerInfo.Api[js.length()];
			ServerInfoApiConverter converter = new ServerInfoApiConverter();
			for (int i=0; i<js.length(); i++) {
				apis[i] = converter.doConvert(js.getJSONObject(i));
			}

			return new ServerInfo(o.getString(ServerInfo.NAME), o.getString(ServerInfo.DESCRIPTION), apis);
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
