package de.stadtrallye.rallyesoft.model.jsonConverter;

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
import de.rallye.model.structures.User;
import de.stadtrallye.rallyesoft.util.IConverter;
import de.stadtrallye.rallyesoft.util.JSONConverter;

/**
 * Created by Ramon on 25.06.13
 */
public class Converters {
	public static class GroupConverter extends JSONConverter<Group> {

		@Override
		public Group doConvert(JSONObject o) throws JSONException {
			return new Group(o.getInt(Group.GROUP_ID),
					o.getString(Group.NAME),
					o.getString(Group.DESCRIPTION));
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
			JSONObject p = o.getJSONObject(Node.POSITION);

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
			return new ServerInfo(o.getString(ServerInfo.NAME), o.getString(ServerInfo.DESCRIPTION));
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
