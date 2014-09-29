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

package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.chat.IChatManager;
import de.stadtrallye.rallyesoft.model.chat.IChatroom;
import de.stadtrallye.rallyesoft.threading.Threading;
import de.stadtrallye.rallyesoft.uimodel.ChatroomPagerAdapter;
import de.stadtrallye.rallyesoft.uimodel.IPicture;
import de.stadtrallye.rallyesoft.widget.SlidingTabLayout;

/**
 * Tab that contains the chat functions (several {@link ChatroomFragment}s)
 * @author Ramon
 *
 */
public class ChatsFragment extends Fragment implements IChatManager.IChatListener {
	
	private static final String THIS = ChatsFragment.class.getSimpleName();

	private List<? extends IChatroom> chatrooms;
	private ViewPager pager;
	private SlidingTabLayout indicator;
	private ChatroomPagerAdapter fragmentAdapter;
//	private int currentTab;
	private IPicture picture = null;
	private IChatManager chatManager;
	private boolean lateInit = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		if (savedInstanceState != null)
//			currentTab = savedInstanceState.getInt(Std.TAB);

		setRetainInstance(true);
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.chat_fragment, container, false);
		
		pager = (ViewPager) v.findViewById(R.id.pager);
		pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin));
		indicator = (SlidingTabLayout) v.findViewById(R.id.indicator);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedBundle) {
		super.onActivityCreated(savedBundle);

		fragmentAdapter = new ChatroomPagerAdapter(getChildFragmentManager(), chatrooms);
		pager.setAdapter(fragmentAdapter);
		indicator.setViewPager(pager);
	}

	private void loadChatrooms() {
		List<? extends IChatroom> newChatrooms = chatManager.getChatrooms();

		if (fragmentAdapter == null) {
			chatrooms = newChatrooms;
			fragmentAdapter = new ChatroomPagerAdapter(getChildFragmentManager(), chatrooms);
			pager.setAdapter(fragmentAdapter);
		} else if (newChatrooms != chatrooms) {
			chatrooms = newChatrooms;
			pager.setAdapter(fragmentAdapter);
			fragmentAdapter.onChatroomsChanged(chatrooms);
			indicator.setViewPager(pager);
		}


		Bundle args = getArguments(); // Open specific Chatroom
		if (args != null) {
			int roomID = args.getInt(Std.CHATROOM, -1);

			if (roomID > -1) {//TODO deactivate this function after executing it?
				int pos = findChatroomPosition(roomID);
				pager.setCurrentItem(pos);
			}
		}
	}

	private int findChatroomPosition(int roomID) {
		int pos = 0;
		for (IChatroom chatroom: chatrooms) {
			if (chatroom.getID() == roomID) {
				break;
			}
			pos++;
		}
		return pos;
	}

	@Override
	public void onStart() {
		super.onStart();

		chatManager = Server.getCurrentServer().acquireChatManager(this);
		chatManager.addListener(this);

//		loadChatrooms();

		if (!chatManager.isChatReady()) {
			chatManager.updateChatrooms();
		} else {
			chatrooms = chatManager.getChatrooms();
			fragmentAdapter.onChatroomsChanged(chatrooms);
		}

	}
	
	@Override
	public void onStop() {
		super.onStop();

//		fragmentAdapter = null;
//		chatrooms = null;

//		currentTab = pager.getCurrentItem();
		chatManager.removeListener(this);
		chatManager = null;
		Server.getCurrentServer().releaseChatManager(this);
	}
	
//	@Override
//	public void onSaveInstanceState(Bundle outState) {
//		super.onSaveInstanceState(outState);
//
////		outState.putInt(Std.TAB, pager.getCurrentItem());
//	}

	/*@Override
	public void pictureTaken(IPicture picture) {
		this.picture = picture;
		updateFragments();


	}*/

	/*private void updateFragments() {// do not push picture, fragment will need to find it itself
		//Display picture in ChatroomFragments
		for (int i=0;i<chatrooms.size();i++) {
			fragmentAdapter.getItem(i).loadImagePreview();
		}
	}*/

	/*@Override
	public IPicture getPicture() {
		return picture;
	}

	@Override
	public void sentPicture() {
		picture = null;
		updateFragments();
	}*/

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onChatroomsChange() {
		chatrooms = chatManager.getChatrooms();
		fragmentAdapter.onChatroomsChanged(chatrooms);
		indicator.setViewPager(pager);
	}

	@Override
	public Handler getCallbackHandler() {
		return Threading.getUiExecutor();
	}
}
