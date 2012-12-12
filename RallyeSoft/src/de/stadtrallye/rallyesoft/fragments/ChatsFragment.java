package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
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
	private ChatFragmentAdapter fragmentAdapter;
	
	
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
		
		if (fragmentAdapter != null) {
			fragmentAdapter.clean(pager);
		}
		
		fragmentAdapter = new ChatFragmentAdapter(getChildFragmentManager(), (model.isLoggedIn())? model.getChatRooms() : null, getString(R.string.unavailable));
		pager.setAdapter(fragmentAdapter);
		indicator.setViewPager(pager);
		
		model.addListener(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		model.removeListener(this);
	}

	@Override
	public void onConnectionStatusChange(boolean newStatus) {
		if (newStatus) {
//			fragmentAdapter.populate(model.getChatRooms(), pager);
//			if (fragmentAdapter != null) {
//				fragmentAdapter.populate(model.getChatRooms(), pager);
//			} else
//				fragmentAdapter = new ChatFragmentAdapter(getChildFragmentManager(), model.getChatRooms(), getString(R.string.unavailable));
//			pager.setAdapter(fragmentAdapter);
//			pager.invalidate();
		} else {
//			fragmentAdapter.clean(pager);
//			main.setVisibility(View.GONE);
//			unavailable.setVisibility(View.VISIBLE);
		}
		
	}
	
	private class ChatFragmentAdapter extends FragmentPagerAdapter{
		
		final private static String FRAGMENT_TITLE = "Chatroom ";

		private int[] chatrooms;
		private FragmentManager manager;
		private String emptyTitle;
		
		public ChatFragmentAdapter(FragmentManager fm, int[] chatrooms, String emptyTitle) {
			super(fm);
			
			manager = fm;
			
			this.chatrooms = chatrooms;
			this.emptyTitle = emptyTitle;
		}
		
		public void populate(int[] chatrooms, ViewGroup container) {
			if (this.chatrooms != null) {
				if (!this.chatrooms.equals(chatrooms)) {
					clean(container);
				} else {
					return;
				}
			}
				
			
			startUpdate(container);
			destroyItem(container, 0, instantiateItem(container, 0));
			
			this.chatrooms = chatrooms;
			
			
			finishUpdate(container);
			container.invalidate();
		}
		
		public void clean(ViewGroup container) {
			if (chatrooms == null)
				return;
			
			startUpdate(container);
			Fragment f;
			FragmentTransaction t = manager.beginTransaction();
			for (int i=0; i<chatrooms.length; ++i) {
				f = (Fragment) instantiateItem(container, i);
				destroyItem(container, i, f);
				t.remove(f);
			}
			t.commit();
			chatrooms = null;
			container.invalidate();
			
			finishUpdate(container);
			container.invalidate();
		}

		@Override
		public Fragment getItem(int pos) {
			Fragment f;
			Bundle b = new Bundle();
			
//			if (chatrooms != null) {
				f = new ChatFragment();
				b.putInt("chatroom", chatrooms[pos]);
//			} else {
//				f = new DummyFragment();
//				b.putInt("layout", R.layout.chat_fragment_unavailable);
//			}
			
			f.setArguments(b);
			return f;
		}

		@Override
		public int getCount() {
//			return (chatrooms != null)? chatrooms.length : 1;
			return chatrooms.length;
		}
		
		@Override
		public CharSequence getPageTitle(int pos) {
//			return (chatrooms != null)? FRAGMENT_TITLE +chatrooms[pos] : emptyTitle;
			return FRAGMENT_TITLE +chatrooms[pos];
		}

	}
	
}
