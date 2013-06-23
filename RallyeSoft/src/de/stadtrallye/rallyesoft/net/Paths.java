package de.stadtrallye.rallyesoft.net;

import de.rallye.model.structures.PictureSize;

public final class Paths {

	public static final String GROUPS = "groups";
	public static final String STATUS = "system/status";
	public static final String CHATS = "chatrooms";
	public static final String MAP_NODES = "map/nodes";
	public static final String MAP_EDGES = "map/edges";
	public static final String CONFIG = "system/config";
	public static final String PICS = "pics";
	public static final String AVATAR = "avatar";
	public static final String INFO = "system/info";
	public static final Object SERVER_PICTURE = "system/picture";

	/**
	 * return relative path to a picture
	 * @param picId PictureID
	 * @param size approximate Size of the requested Picture
	 * @return
	 */
	public static final String getPic(int picId, PictureSize size) {
		return PICS +"/"+ picId +"/"+ size.toShortString();
	}

	public static final String getAvatar(int groupID) {
		return GROUPS +"/"+ groupID +"/"+ AVATAR;
	}
}
