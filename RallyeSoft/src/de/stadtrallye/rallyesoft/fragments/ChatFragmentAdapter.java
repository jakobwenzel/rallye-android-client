package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class ChatFragmentAdapter extends FragmentPagerAdapter{
	
	final private static String FRAGMENT_TITLE = "Chatroom ";

	private int[] chatrooms;
	
	public ChatFragmentAdapter(FragmentManager fm, int[] chatrooms) {
		super(fm);
		
		this.chatrooms = chatrooms;
	}

	@Override
	public Fragment getItem(int pos) {
//		Fragment f = manager.findFragmentByTag(FRAGMENT_TAG +pos);
		
		Fragment f = new ChatFragment();
		Bundle b = new Bundle();
		b.putInt("chatroom", chatrooms[pos]);
		f.setArguments(b);
		
		return f;
	}

	@Override
	public int getCount() {
		return chatrooms.length;
	}
	
	@Override
	public CharSequence getPageTitle(int pos) {
		return FRAGMENT_TITLE +chatrooms[pos];
	}

}
