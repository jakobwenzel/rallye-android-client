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

import de.rallye.model.structures.PictureSize;

public final class Paths {

	public static final String GROUPS = "groups";
	public static final String STATUS = "system/status";
	public static final String CHAT = "chat";
	public static final String CHATROOMS = CHAT+"/rooms";
	public static final String MAP_NODES = "games/rallye/map/nodes";
	public static final String MAP_EDGES = "games/rallye/map/edges";
	public static final String MAP_CONFIG = "games/rallye/map/config";
	public static final String PICS = "resources/pics";
	public static final String SERVER_INFO = "server/info";
	public static final String SERVER_PICTURE = "server/picture";
	public static final String SERVER_STATUS = "server/status";
	public static final String USERS = "users";
	public static final String TASKS = "games/rallye/tasks";
	public static final String TASKS_SUBMISSIONS_ALL = "games/rallye/tasks/all";

	public static final String PARAM_GROUP_ID = "groupID";
	public static final String PARAM_CHATROOM_ID = "chatroomID";
	public static final String PARAM_SINCE = "since";
	public static final String PARAM_TASK_ID = "taskID";

	public static final String CHATROOM_CHATS = CHATROOMS +"/{"+PARAM_CHATROOM_ID+"}";
	public static final String CHATROOM_CHATS_SINCE = CHATROOM_CHATS +"/since/{"+PARAM_SINCE+"}";
	public static final String GROUPS_WITH_ID = GROUPS + "/{"+PARAM_GROUP_ID+"}";
	public static final String TASK_SUBMISSIONS = TASKS + "/{"+PARAM_TASK_ID+"}";



	/**
	 * return relative path to a picture
	 * @param picId PictureID
	 * @param size approximate Size of the requested Picture
	 * @return a RallyeServer compliant relative path (without the base URL)
	 */
	public static String getPic(int picId, PictureSize size) {
		return PICS +"/"+ picId +"/"+ size.toShortString();
	}

	public static final String SNIPPET_AVATAR = "avatar";
	public static final String SNIPPET_SINCE = "since";

	public static String getAvatar(int groupID) {
		return GROUPS +"/"+ groupID +"/"+ SNIPPET_AVATAR;
	}

}
