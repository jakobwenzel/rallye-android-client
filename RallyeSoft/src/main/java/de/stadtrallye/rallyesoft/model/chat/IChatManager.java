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
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.model.chat;

import java.util.List;

import de.stadtrallye.rallyesoft.model.IHandlerCallback;

/**
 * Created by Ramon on 22.09.2014.
 */
public interface IChatManager {
	boolean isChatReady();

	List<? extends IChatroom> getChatrooms();

	void updateChatrooms();

	void forceRefreshChatrooms();

	void addListener(IChatListener chatListener);
	void removeListener(IChatListener chatListener);

	IChatroom findChatroom(int roomID);

	interface IChatListener extends IHandlerCallback {
		void onChatroomsChange();
	}
}
