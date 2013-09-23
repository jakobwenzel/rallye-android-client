package de.stadtrallye.rallyesoft.net;

import de.rallye.model.structures.PictureSize;

public final class Paths {

	public static final String GROUPS = "groups";
	public static final String STATUS = "system/status";
	public static final String CHATS = "chatrooms";
	public static final String MAP_NODES = "map/nodes";
	public static final String MAP_EDGES = "map/edges";
	public static final String MAP_CONFIG = "map/config";
	public static final String PICS = "pics";
	public static final String AVATAR = "avatar";
	public static final String INFO = "system/info";
	public static final String SERVER_PICTURE = "system/picture";
	public static final String USERS = "users";
	public static final String TASKS = "tasks";
	public static final String SUBMISSIONS = "tasks/all";

	/**
	 * return relative path to a picture
	 * @param picId PictureID
	 * @param size approximate Size of the requested Picture
	 * @return a RallyeServer compliant relative path (without the base URL)
	 */
	public static String getPic(int picId, PictureSize size) {
		return PICS +"/"+ picId +"/"+ size.toShortString();
	}

	public static String getAvatar(int groupID) {
		return GROUPS +"/"+ groupID +"/"+ AVATAR;
	}
}
