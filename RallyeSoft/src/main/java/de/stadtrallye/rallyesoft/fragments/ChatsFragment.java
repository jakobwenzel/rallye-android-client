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

package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import java.util.List;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.ChatroomPagerAdapter;
import de.stadtrallye.rallyesoft.uimodel.IPicture;

import static de.stadtrallye.rallyesoft.model.Model.getModel;

/**
 * Tab that contains the chat functions (several {@link ChatroomFragment}s)
 * @author Ramon
 *
 */
public class ChatsFragment extends Fragment {
	
	private static final String THIS = ChatsFragment.class.getSimpleName();

	private List<? extends IChatroom> chatrooms;
	private ViewPager pager;
	private PagerSlidingTabStrip indicator;
	private ChatroomPagerAdapter fragmentAdapter;
//	private int currentTab;
	private IPicture picture = null;
	private IModel model;

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
		indicator = (PagerSlidingTabStrip) v.findViewById(R.id.indicator);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedBundle) {
		super.onActivityCreated(savedBundle);
		loadChatrooms();

	}

	private void loadChatrooms() {
		model = getModel(getActivity());
		chatrooms = model.getChatrooms();

		fragmentAdapter = new ChatroomPagerAdapter(getChildFragmentManager(), chatrooms);
		pager.setAdapter(fragmentAdapter);
		indicator.setViewPager(pager);


		Bundle args = getArguments(); // Open specific Chatroom
		if (args != null) {
			int room = args.getInt(Std.CHATROOM, -1);

			if (room > -1) {
				int pos = chatrooms.indexOf(model.getChatroom(room));
				pager.setCurrentItem(pos);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		if (model!=getModel(getActivity()))
			loadChatrooms();

	}
	
	@Override
	public void onStop() {
		super.onStop();

//		fragmentAdapter = null;
//		chatrooms = null;

//		currentTab = pager.getCurrentItem();
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

	}


	

}
