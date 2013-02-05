package de.stadtrallye.rallyesoft.fragments;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.viewpagerindicator.TitlePageIndicator;

import de.stadtrallye.rallyesoft.IModelActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.Std;
import de.stadtrallye.rallyesoft.model.Chatroom;
import de.stadtrallye.rallyesoft.model.IConnectionStatusListener;
import de.stadtrallye.rallyesoft.model.IModel.ConnectionStatus;
import de.stadtrallye.rallyesoft.model.Model;

/**
 * Tab that contains the chat functions (several {@link ChatroomFragment}s)
 * If not logged in will use {@link DummyAdapter} instead of {@link ChatAdapter} to display special tab
 * @author Ramon
 *
 */
public class ChatsFragment extends BaseFragment implements IConnectionStatusListener {
	
	private Model model;
	private ViewPager pager;
	private TitlePageIndicator indicator;
	private FragmentPagerAdapter fragmentAdapter;
	private List<Chatroom> chatrooms;
	private int currentTab = 0;
	
	
	public ChatsFragment() {
		
		THIS = ChatsFragment.class.getSimpleName();
		
		if (DEBUG)
			Log.v(THIS, "Instantiated "+ this.toString());
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
			currentTab = savedInstanceState.getInt(Std.TAB);
//		setRetainInstance(false);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v;
		v = inflater.inflate(R.layout.chat_fragment, container, false);

        pager = (ViewPager)v.findViewById(R.id.pager);
        pager.setPageMargin(5);

        indicator = (TitlePageIndicator)v.findViewById(R.id.indicator);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		try {
			model = ((IModelActivity) getActivity()).getModel();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity");
		}
		
		onConnectionStatusChange(model.getConnectionStatus());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		model.addListener(this);
	}
	
//	@Override
//	public void onResume() {
//		super.onResume();
//		
//		
//	}
	
//	@Override
//	public void onPause() {
//		super.onPause();
//		
//		
//	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		model.removeListener(this);
		currentTab = pager.getCurrentItem();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt(Std.TAB, pager.getCurrentItem());
	}
	
	private void populateChats() {
		chatrooms = model.getChatrooms();
		fragmentAdapter = new ChatFragmentAdapter(getChildFragmentManager(), chatrooms);
		pager.setAdapter(fragmentAdapter);
		indicator.setViewPager(pager);
		indicator.setCurrentItem(currentTab);
	}
	
	private void chatsUnavailable() {
		chatrooms = null;
		fragmentAdapter = new DummyAdapter(getChildFragmentManager());
		pager.setAdapter(fragmentAdapter);
		indicator.setViewPager(pager);
		indicator.invalidate();
	}

	@Override
	public void onConnectionStatusChange(ConnectionStatus newStatus) {
		if (newStatus == ConnectionStatus.Connected) {
			populateChats();
		} else {
			chatsUnavailable();
		}
	}
	
	@Override
	public void onConnectionFailed(Exception e, ConnectionStatus lastStatus) {
		onConnectionStatusChange(lastStatus);
	}
	
	private class ChatFragmentAdapter extends FragmentPagerAdapter {
		
		final private static String FRAGMENT_TITLE = "Chatroom ";

		private List<Chatroom> chatrooms;
		
		public ChatFragmentAdapter(FragmentManager fm, List<Chatroom> chatrooms) {
			super(fm);
			
			this.chatrooms = chatrooms;
		}
		
		/**
		 * Needed so the FragmentManager can distinguish tabs of different chatrooms (, if re logging in as different user)
		 * Default behavior, will name Fragments after there position
		 */
		@Override
		public long getItemId(int position) {
			return chatrooms.get(position).getID();
		}

		@Override
		public Fragment getItem(int pos) {
			Fragment f;
			
			f = new ChatroomFragment(chatrooms.get(pos));
			
			return f;
		}

		@Override
		public int getCount() {
			return chatrooms.size();
		}
		
		@Override
		public CharSequence getPageTitle(int pos) {
			return FRAGMENT_TITLE +chatrooms.get(pos).getID();
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
			b.putInt(DummyFragment.LAYOUT, R.layout.chat_unavailable);
			f.setArguments(b);
			return f;
		}
		
		/**
		 * So as not be confused with actual chatrooms in FragmentManager
		 * @see ChatFragmentAdapter
		 */
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
