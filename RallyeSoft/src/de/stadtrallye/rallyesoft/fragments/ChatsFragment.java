package de.stadtrallye.rallyesoft.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

import java.util.LinkedList;
import java.util.List;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;

/**
 * Tab that contains the chat functions (several {@link ChatroomFragment}s)
 * @author Ramon
 *
 */
public class ChatsFragment extends SherlockFragment {
	
	private static final String THIS = ChatsFragment.class.getSimpleName();
	
	private IModel model;
	private ViewPager pager;
	private PagerSlidingTabStrip indicator;
	private ChatFragmentAdapter fragmentAdapter;
	private List<? extends IChatroom> chatrooms;
	private int currentTab = 0;
	private SlidingMenu slidingMenu;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
			currentTab = savedInstanceState.getInt(Std.TAB);
		
		fragmentAdapter = new ChatFragmentAdapter(getChildFragmentManager());
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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		try {
			model = ((IModelActivity) getActivity()).getModel();
			slidingMenu = ((SlidingFragmentActivity) getActivity()).getSlidingMenu();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity and extend SlidingFragmentActivity");
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		chatrooms = model.getChatrooms();

		pager.setAdapter(fragmentAdapter);
		indicator.setViewPager(pager);
		indicator.setOnPageChangeListener(new SlidingMenuHelper(slidingMenu));
		
		pager.setCurrentItem(currentTab);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		
		currentTab = pager.getCurrentItem();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt(Std.TAB, pager.getCurrentItem());
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem refreshMenuItem = menu.add(Menu.NONE, R.id.refresh_menu, 30, R.string.refresh);
		
		refreshMenuItem.setIcon(R.drawable.refresh);
		refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		MenuItem pictureMenuItem = menu.add(Menu.NONE, R.id.picture_menu, 10, R.string.photo);

		pictureMenuItem.setIcon(R.drawable.camera);
		pictureMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh_menu:
			chatrooms.get(pager.getCurrentItem()).refresh();
			return true;
		case R.id.picture_menu: //Open a chooser containing all apps that can pick a jpeg and the camera
			//Attention: Our RequestCode will not be used for the result, if a jpeg is picked, data.getType will contain image/jpeg, if the picture was just taken with the camera it will be null
			Intent pickIntent = new Intent();
			pickIntent.setType("image/jpeg");
			pickIntent.setAction(Intent.ACTION_GET_CONTENT);

			Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

			Intent chooserIntent = Intent.createChooser(pickIntent, getString(R.string.select_take_picture));
			chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});

			startActivityForResult(chooserIntent, Std.PICK_IMAGE);
			return true;
		default:
			Log.d(THIS, "No hit on menu item "+ item);
			return false;
		}
	}
	
	private class ChatFragmentAdapter extends FragmentPagerAdapter {
		
		public ChatFragmentAdapter(FragmentManager fm) {
			super(fm);
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
		public Fragment getItem(int pos) {
			ChatroomFragment f;

			f = new ChatroomFragment();
			Bundle b = new Bundle();
			b.putInt(Std.CHATROOM, chatrooms.get(pos).getID());
			f.setArguments(b);

			return f;
		}

		@Override
		public int getCount() {
			return chatrooms.size();
		}
		
		@Override
		public int getItemPosition(Object object) {
			return POSITION_UNCHANGED;
		}
		
		@Override
		public CharSequence getPageTitle(int pos) {
			return chatrooms.get(pos).getName();
		}

	}
	
	private class SlidingMenuHelper implements OnPageChangeListener {
		
		private SlidingMenu slidingMenu;

		public SlidingMenuHelper(SlidingMenu slidingMenu) {
			this.slidingMenu = slidingMenu;
		}
		
		@Override
		public void onPageScrollStateChanged(int arg0) { }

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) { }

		@Override
		public void onPageSelected(int position) {
			switch (position) {
			case 0:
				slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
				break;
			default:
				slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
				break;
			}
		}
	}
}
