package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.viewpagerindicator.TitlePageIndicator;

import de.stadtrallye.rallyesoft.IModelActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModelListener;
import de.stadtrallye.rallyesoft.model.Model;

/**
 * Tab that contains the chat functions (several chatrooms)
 * @author Ramon
 *
 */
public class ChatsFragment extends SherlockFragment implements IModelListener {
	
	private Model model;
	private ViewPager pager;
	private TitlePageIndicator indicator;
	private FragmentPagerAdapter fragmentAdapter;
	private int[] currentRooms;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v;
		v = inflater.inflate(R.layout.chat_fragment, container, false);

        pager = (ViewPager)v.findViewById(R.id.pager);

        indicator = (TitlePageIndicator)v.findViewById(R.id.indicator);
		
		return v;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		try {
			model = ((IModelActivity) getActivity()).getModel();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity");
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		Log.v("ChatsFragment", "ChatFragment started");
		
		onConnectionStatusChange(model.isLoggedIn());
		
		model.addListener(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		model.removeListener(this);
	}
	
	private void populateChats() {
		if (!model.getChatRooms().equals(currentRooms)) {
			currentRooms = model.getChatRooms();
			fragmentAdapter = new ChatFragmentAdapter(getChildFragmentManager(), currentRooms);
			pager.setAdapter(fragmentAdapter);
			indicator.setViewPager(pager);
			indicator.invalidate();
		}
	}
	
	private void chatsUnavailable() {
		if (!(fragmentAdapter instanceof DummyAdapter)) {
			currentRooms = null;
			fragmentAdapter = new DummyAdapter(getChildFragmentManager());
			pager.setAdapter(fragmentAdapter);
			indicator.setViewPager(pager);
			indicator.invalidate();
		}
	}

	@Override
	public void onConnectionStatusChange(boolean newStatus) {
		if (newStatus) {
			populateChats();
		} else {
			chatsUnavailable();
		}
		
	}
	
	private class ChatFragmentAdapter extends FragmentPagerAdapter {
		
		final private static String FRAGMENT_TITLE = "Chatroom ";

		private int[] chatrooms;
		
		public ChatFragmentAdapter(FragmentManager fm, int[] chatrooms) {
			super(fm);
			
			this.chatrooms = chatrooms;
		}
		
		@Override
		public long getItemId(int position) {
			return chatrooms[position];
		}

		@Override
		public Fragment getItem(int pos) {
			Fragment f;
			Bundle b = new Bundle();
			
			f = new ChatFragment();
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
	
	private class DummyAdapter extends FragmentPagerAdapter {

		public DummyAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int arg0) {
			Fragment f = new DummyFragment();
			Bundle b = new Bundle();
			b.putInt(DummyFragment.LAYOUT, R.layout.chat_fragment_unavailable);
			f.setArguments(b);
			return f;
		}
		
		@Override
		public long getItemId(int position) {
			return -1;
		}

		@Override
		public int getCount() {
			return 1;
		}
		
	}
	
}
