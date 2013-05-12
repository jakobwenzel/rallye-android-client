package de.stadtrallye.rallyesoft.fragments;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.UIComm.IModelActivity;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IChatroom;
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
//	private TitlePageIndicator indicator;
	private PagerSlidingTabStrip indicator;
	private ChatFragmentAdapter fragmentAdapter;
	private List<? extends IChatroom> chatrooms;
	private int currentTab = 0;
	private MenuItem refreshMenuItem;
	private MenuItem pictureMenuItem;
	
	
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
		
		fragmentAdapter = new ChatFragmentAdapter(getChildFragmentManager());
		setRetainInstance(true);
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v;
		v = inflater.inflate(R.layout.chat_fragment, container, false);

        pager = (ViewPager)v.findViewById(R.id.pager);
        pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin));

//        indicator = (TitlePageIndicator)v.findViewById(R.id.indicator);
        indicator = (PagerSlidingTabStrip)v.findViewById(R.id.indicator);
        
        pager.setAdapter(fragmentAdapter);
        
        indicator.setViewPager(pager);
		
		return v;
	}
	
//	<com.viewpagerindicator.TitlePageIndicator
//  	android:id="@+id/indicator"
//  	android:padding="10dip"
//  	android:layout_height="wrap_content"
//  	android:layout_width="match_parent"
//		android:textColor="@color/abs__bright_foreground_disabled_holo_light"
//		app:selectedColor="@color/foreground_grey"
//  />
	
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
		
		onConnectionStatusChange(model.getConnectionStatus());
//		indicator.setCurrentItem(currentTab);
//		pager.setCurrentItem(currentTab);
		
		model.addListener(this);
	}
	
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
	
//	private static final int REFRESH_ID = -199;
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		refreshMenuItem = menu.add(Menu.NONE, R.id.refresh_menu, Menu.NONE, R.string.refresh);
		
		refreshMenuItem.setIcon(R.drawable.refresh);
		refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		pictureMenuItem = menu.add(Menu.NONE, R.id.picture_menu, Menu.NONE, R.string.photo);
		
		pictureMenuItem.setIcon(R.drawable.camera);
		pictureMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh_menu:
			chatrooms.get(pager.getCurrentItem()).refresh();//TODO disable if no chatrooms loaded/offline
			return true;
		case R.id.picture_menu:
			Intent pickIntent = new Intent();
			pickIntent.setType("image/*");
			pickIntent.setAction(Intent.ACTION_GET_CONTENT);

			Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
//			Uri out = getOutputMediaFileUri(null); // create a file to save the image
//		    takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, out); // set the image file name

			Intent chooserIntent = Intent.createChooser(pickIntent, getString(R.string.select_take_picture));
			chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});

			startActivityForResult(chooserIntent, Std.PICK_IMAGE);
			return true;
		default:
			Log.d(THIS, "No hit on menu item "+ item);
			return false;
		}
	}

	@Override
	public void onConnectionStatusChange(ConnectionStatus newStatus) {
		if (newStatus == ConnectionStatus.Connected) {
			chatrooms = model.getChatrooms();
			fill();
			refreshMenuItem.setVisible(true);
			pictureMenuItem.setVisible(true);
		} else {
			chatrooms = null;
			clear();
			refreshMenuItem.setVisible(false);
			pictureMenuItem.setVisible(false);
		}
	}
	
	@Override
	public void onConnectionFailed(Exception e, ConnectionStatus lastStatus) {
		onConnectionStatusChange(lastStatus);
	}
	
	public void fill() {
		fragmentAdapter.enable = true;
		fragmentAdapter.notifyDataSetChanged();
		indicator.notifyDataSetChanged();
	}
	
	public void clear() {
		fragmentAdapter.enable = false;
		fragmentAdapter.notifyDataSetChanged();
		indicator.notifyDataSetChanged();
	}
	
	private class ChatFragmentAdapter extends FragmentPagerAdapter {

		private boolean enable = false;
		
		public ChatFragmentAdapter(FragmentManager fm) {
			super(fm);
		}
		
		/**
		 * Needed so the FragmentManager can distinguish tabs of different chatrooms (, if re logging in as different user)
		 * Default behavior, will name Fragments after their position
		 */
		@Override
		public long getItemId(int position) {
			return (enable)? chatrooms.get(position).getID() : -1;
		}

		@Override
		public Fragment getItem(int pos) {
			if (enable)
				return getChatFragment(pos);
			else 
				return getDummyFragment();
		}
		
		private Fragment getDummyFragment() {
			Fragment f = new DummyFragment();
			Bundle b = new Bundle();
			b.putInt(Std.LAYOUT, R.layout.chat_unavailable);
			f.setArguments(b);
			return f;
		}
		
		private Fragment getChatFragment(int pos) {
			ChatroomFragment f;
			
			f = new ChatroomFragment();
			Bundle b = new Bundle();
			b.putInt(Std.CHATROOM, chatrooms.get(pos).getID());
			f.setArguments(b);
			
			return f;
		}

		@Override
		public int getCount() {
			return (enable)? chatrooms.size() : 1;
		}
		
		@Override
		public int getItemPosition(Object object) {
			if (enable && object instanceof DummyFragment)
				return POSITION_NONE;
			else if (!enable && object instanceof ChatroomFragment)
				return POSITION_NONE;
			else
				return POSITION_UNCHANGED;
		}
		
		@Override
		public CharSequence getPageTitle(int pos) {
			return (enable)? chatrooms.get(pos).getName() : "";
		}

	}
}
