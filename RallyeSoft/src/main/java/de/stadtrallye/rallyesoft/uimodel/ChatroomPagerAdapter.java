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

package de.stadtrallye.rallyesoft.uimodel;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.util.List;

import de.stadtrallye.rallyesoft.fragments.ChatroomFragment;
import de.stadtrallye.rallyesoft.model.chat.IChatroom;

/**
 * Adapter for a List<IChatroom>
 */
public class ChatroomPagerAdapter extends FragmentPagerAdapter {

	private List<? extends IChatroom> chatrooms;
	private ChatroomFragment current;

	public ChatroomPagerAdapter(FragmentManager fm, List<? extends IChatroom> chatrooms) {
		super(fm);
		this.chatrooms = chatrooms;
	}

	/**
	 * Needed so the FragmentManager can distinguish tabs of different chatrooms (, if re logging in as different user)
	 * Default behavior, will name Fragments after their position
	 */
	@Override
	public long getItemId(int position) {
		return chatrooms.get(position).getID();
	}

	@Override
	public ChatroomFragment getItem(int pos) {
		return ChatroomFragment.newInstance(chatrooms.get(pos).getID());
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object) {
		super.setPrimaryItem(container, position, object);

		current = (ChatroomFragment) object;
	}

	@Override
	public int getCount() {
		return (chatrooms != null)? chatrooms.size() : 0;
	}

	@Override
	public CharSequence getPageTitle(int pos) {
		return chatrooms.get(pos).getName();
	}
}
