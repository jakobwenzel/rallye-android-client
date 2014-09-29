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

package de.stadtrallye.rallyesoft.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.rallye.model.structures.GroupUser;

/**
 * Common:ChatEntry enhanced by group name, user name and if one or both matches the currently logged in user
 * If a name is not available, the correspondent id is returned as String
 */
public class ChatEntry extends de.rallye.model.structures.ChatEntry {

	private String userName;
	private String groupName;

	public enum Sender { Me, MyGroup, SomeoneElse}

	public ChatEntry(int chatID, String message, long timestamp, int groupID, String groupName, int userID, String userName, Integer pictureID) {
		this(chatID, message, timestamp, groupID, userID, pictureID);

		this.groupName = groupName;
		this.userName = userName;
	}

	@JsonCreator
	public ChatEntry(@JsonProperty("chatID") int chatID, @JsonProperty("message") String message, @JsonProperty("timestamp") long timestamp, @JsonProperty("groupID") int groupID, @JsonProperty("userID") int userID, @JsonProperty("pictureID") Integer pictureID) {
		super(chatID, message, timestamp, groupID, userID, (pictureID == 0)? null : pictureID);//why?
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getUserName() {
		return (userName != null)? userName : String.valueOf(userID);
	}

	public String getGroupName() {
		return (groupName != null)? groupName : String.valueOf(groupID);
	}
	
// --Commented out by Inspection START (22.09.13 02:44):
//	public Sender getSender(GroupUser user) {
//		return getSender(user, this.groupID, this.userID);
//	}
// --Commented out by Inspection STOP (22.09.13 02:44)

	public static Sender getSender(GroupUser user, int groupID, int userID) {
		if (userID == user.userID) {
			return Sender.Me;
		} else if (groupID == user.groupID) {
			return Sender.MyGroup;
		} else {
			return Sender.SomeoneElse;
		}
	}
}
