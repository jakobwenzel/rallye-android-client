package de.stadtrallye.rallyesoft.net;

public final class Paths {

	public static final String GROUPS = "groups";
	public static final String STATUS = "system/status";
	public static final String CHATS = "chatrooms";
	public static final String MAP_NODES = "map/nodes";
	public static final String MAP_EDGES = "map/edges";
	public static final String CONFIG = "system/config";
	public static final String PICS = "pics";
	
	/**
	 * return relative path to a picture
	 * @param picId
	 * @param picSize t, m, l
	 * @return
	 */
	public static final String getPic(int picId, char picSize) {
		return PICS +"/"+ picId +"/"+ picSize;
	}
}
