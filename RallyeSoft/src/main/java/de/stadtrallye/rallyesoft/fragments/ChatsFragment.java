package de.stadtrallye.rallyesoft.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;

import java.util.List;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.ChatroomPagerAdapter;
import de.stadtrallye.rallyesoft.uimodel.IPictureTakenListener;
import de.stadtrallye.rallyesoft.util.ImageLocation;

import static de.stadtrallye.rallyesoft.model.Model.getModel;
import static de.stadtrallye.rallyesoft.uimodel.TabManager.getTabManager;

/**
 * Tab that contains the chat functions (several {@link ChatroomFragment}s)
 * @author Ramon
 *
 */
public class ChatsFragment extends SherlockFragment implements IPictureTakenListener {
	
	private static final String THIS = ChatsFragment.class.getSimpleName();

	private List<? extends IChatroom> chatrooms;
	private ViewPager pager;
	private PagerSlidingTabStrip indicator;
	private ChatroomPagerAdapter fragmentAdapter;
//	private int currentTab;
	private Picture picture = null;
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

	@Override
	public void pictureTaken(Picture picture) {
		this.picture = picture;
		updateFragments();


	}

	private void updateFragments() {
		//Display picture in ChatroomFragments
		for (int i=0;i<chatrooms.size();i++) {
			fragmentAdapter.getItem(i).loadImagePreview();
		}
	}

	@Override
	public Picture getPicture() {
		return picture;
	}

	@Override
	public void sentPicture() {
		picture = null;
		updateFragments();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem refreshMenuItem = menu.add(Menu.NONE, R.id.refresh_menu, 30, R.string.refresh);
		
		refreshMenuItem.setIcon(R.drawable.ic_refresh_light);
		refreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		
		MenuItem pictureMenuItem = menu.add(Menu.NONE, R.id.picture_menu, 10, R.string.take_picture);

		pictureMenuItem.setIcon(R.drawable.ic_camera_light);
		pictureMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean drawerOpen = getTabManager(getActivity()).isMenuOpen();

		menu.findItem(R.id.refresh_menu).setVisible(!drawerOpen);
		menu.findItem(R.id.picture_menu).setVisible(!drawerOpen);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh_menu:
			chatrooms.get(pager.getCurrentItem()).refresh();
			return true;
		case R.id.picture_menu: //Open a chooser containing all apps that can pick a jpeg and the camera
			ImageLocation.startPictureTakeOrSelect(getActivity());
			return true;
		default:
			Log.d(THIS, "No hit on menu item "+ item);
			return false;
		}
	}
}
