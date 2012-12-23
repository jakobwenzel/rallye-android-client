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
import de.stadtrallye.rallyesoft.model.IConnectionStatusListener;
import de.stadtrallye.rallyesoft.model.Model;

/**
 * Tab that contains the chat functions (several chatrooms)
 * If not logged in will use {@link DummyAdapter} instead of {@link ChatAdapter} to display special tab
 * @author Ramon
 *
 */
public class ChatsFragment extends BaseFragment implements IConnectionStatusListener {
	
	private Model model;
	private ViewPager pager;
	private TitlePageIndicator indicator;
	private FragmentPagerAdapter fragmentAdapter;
	private List<Integer> currentRooms;
	private int initialTab = 0;
	
	
	public ChatsFragment() {
		
		THIS = ChatsFragment.class.getSimpleName();
		
		if (DEBUG)
			Log.v(THIS, "Instantiated "+ this.toString());
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(false);
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
		
		onConnectionStatusChange(model.isLoggedIn());
		
		model.addListener(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		pager.setCurrentItem(initialTab);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		initialTab = pager.getCurrentItem();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		model.removeListener(this);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt(Std.TAB, pager.getCurrentItem());
	}
	
	private void populateChats() {
//		if (!Arrays.equals(model.getChatRooms(), currentRooms)) {
			currentRooms = model.getChatRooms();
			fragmentAdapter = new ChatFragmentAdapter(getChildFragmentManager(), currentRooms);
			pager.setAdapter(fragmentAdapter);
			pager.setCurrentItem(initialTab);
			indicator.setViewPager(pager);
			indicator.invalidate();
//		}
	}
	
	private void chatsUnavailable() {
//		if (/*!(fragmentAdapter instanceof DummyAdapter)*/true) {
			currentRooms = null;
			FragmentManager childManager = getChildFragmentManager();
//			childManager.enableDebugLogging(true);
			fragmentAdapter = new DummyAdapter(childManager);
			pager.setAdapter(fragmentAdapter);
			indicator.setViewPager(pager);
			indicator.invalidate();
//		}
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

		private List<Integer> chatrooms;
		
		public ChatFragmentAdapter(FragmentManager fm, List<Integer> currentRooms) {
			super(fm);
			
			this.chatrooms = currentRooms;
		}
		
		/**
		 * Needed so the FragmentManager can distinguish tabs of different chatrooms (, if re logging in as different user)
		 * Default behavior, will name Fragments after there position
		 */
		@Override
		public long getItemId(int position) {
			return chatrooms.get(position);
		}

		@Override
		public Fragment getItem(int pos) {
			Fragment f;
			Bundle b = new Bundle();
			
			f = new ChatroomFragment();
			b.putInt("chatroom", chatrooms.get(pos));
			
			f.setArguments(b);
			return f;
		}

		@Override
		public int getCount() {
			return chatrooms.size();
		}
		
		@Override
		public CharSequence getPageTitle(int pos) {
			return FRAGMENT_TITLE +chatrooms.get(pos);
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
