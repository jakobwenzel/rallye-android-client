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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import de.rallye.model.structures.SimpleChatWithPictureHash;
import de.stadtrallye.rallyesoft.PictureGalleryActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.chat.ChatEntry;
import de.stadtrallye.rallyesoft.model.chat.IChatroom;
import de.stadtrallye.rallyesoft.threading.Threading;
import de.stadtrallye.rallyesoft.uimodel.ChatCursorAdapter;
import de.stadtrallye.rallyesoft.uimodel.IPicture;
import de.stadtrallye.rallyesoft.uimodel.IPictureHandler;
import de.stadtrallye.rallyesoft.uimodel.IProgressUI;
import de.stadtrallye.rallyesoft.util.ImageLocation;

import static de.stadtrallye.rallyesoft.uimodel.TabManager.getTabManager;

/**
 * One Chatroom, with input methods
 * @author Ramon
 *
 */
public class ChatroomFragment extends Fragment implements IChatroom.IChatroomListener, OnClickListener, OnItemClickListener, AbsListView.OnScrollListener {

	private static final String THIS = ChatroomFragment.class.getSimpleName();
	private static final boolean DEBUG = false;
	

	private int roomID;
	private int[] lastPos = null; //[0] = line, [1] = px
	private IChatroom chatroom;
	
	private IProgressUI ui; // access to IndeterminateProgress in ActionBar
	
	private ListView list;	//List of ChatEntries
	private ChatCursorAdapter chatAdapter; //Adapter for List
	private ImageButton send;
	private EditText text;
	private ProgressBar loading;
	private ImageView chosen_picture;
	private IPictureHandler pictureHandler;


	/**
	 * retain savedInstanceState for when creating the list (ScrollState)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null)
			roomID = args.getInt(Std.CHATROOM, -1);
		
		if (savedInstanceState != null)
			lastPos = savedInstanceState.getIntArray(Std.LAST_POS);
		else
			lastPos = new int[2];

		setHasOptionsMenu(true);

		chatroom = Server.getCurrentServer().acquireChatManager(this).findChatroom(roomID);
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
				chatroom.update();
				return true;
			case R.id.picture_menu: //Open a chooser containing all apps that can pick a jpeg and the camera
				ImageLocation.startPictureTakeOrSelect(getActivity(), chatroom.getID());
				return true;
			default:
				return false;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.chat_list, container, false);
		list = (ListView) v.findViewById(R.id.chat_list);
		send = (ImageButton) v.findViewById(R.id.button_send);
		text = (EditText) v.findViewById(R.id.edit_new_message);
		loading = (ProgressBar) v.findViewById(R.id.loading);
		chosen_picture = (ImageView) v.findViewById(R.id.picture_chosen);

		send.setOnClickListener(this);
		list.setOnItemClickListener(this);

		return v;
	}

	private void loadChats() {
		if (chatroom == null) {
			throw new IllegalArgumentException(THIS +" could not find the Model of Chatroom "+ roomID);
		}

		chatAdapter = new ChatCursorAdapter(getActivity(), Server.getCurrentServer().getPictureResolver(), chatroom);
		list.setAdapter(chatAdapter);
		list.setOnScrollListener(this);//TODO: use chatroom.getLastReadId() (wrap cursorAdapter add extra line)

		loadImagePreview();

		chatroom.addListener(this);
		chatAdapter.changeCursor(chatroom.getChatCursor());

		restoreLastReadId(chatroom.getLastReadId());
	}

	private void loadImagePreview() {
		IPicture pic = pictureHandler.getPicture();
		if (pic != null && pic.getSource() == chatroom.getID()) {//TODO: use IPicture.UploadState
			chosen_picture.setVisibility(View.VISIBLE);
			ImageLoader.getInstance().displayImage(pic.getPath().toString(), chosen_picture);
		} else {
			chosen_picture.setVisibility(View.GONE);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			ui = (IProgressUI) getActivity();
			pictureHandler = (IPictureHandler) getActivity();

			loadChats();

		} catch (ClassCastException e) {
			Log.i(THIS, "Activity must implement IProgressUI and IPictureHandler");
			throw e;
		}
	}

	/**
	 * if we are logged in show progress and start asynchronous chat refresh
	 */
	@Override
	public void onStart() {
		super.onStart();

		loadImagePreview();
	}
	
	@Override
	public void onStop() {
		super.onStop();

		saveScrollState();
		
		chatroom.setLastReadId(chatAdapter.getChatID(lastPos[0]));
		
		chatroom.removeListener(this);
	}

	/**
	 * save into lastPos[]: 0: nr, 1: pixel
	 */
	private void saveScrollState() {
        if (lastPos==null)
            lastPos = new int[2];
        lastPos[0] = list.getFirstVisiblePosition();

        if (lastPos[0] == 0) {
            lastPos[1] = 0;
        } else {
            View v = list.getChildAt(0);
            lastPos[1] = v.getTop();
        }
	}

	private void restoreLastReadId(int chatId) {
		list.setSelectionFromTop(chatAdapter.findPos(chatId), 0);
	}
	
	private void restoreScrollState() {
		if (lastPos != null) {
	    	list.setSelectionFromTop(lastPos[0], lastPos[1]);
	    	if (DEBUG)
				Log.v(THIS, "ScrollState restored: "+ lastPos[0]);
        } else
        	list.setSelection(list.getCount()-1);
	}

	@Override
	public void onChatsChanged() {
		chatAdapter.changeCursor(chatroom.getChatCursor());
	}

	@Override
	public void onStateChanged(IChatroom.ChatroomState newStatus) {
		switch (newStatus) {
		case Refreshing:
			ui.activateProgressAnimation();
			break;
		case Ready:
			ui.deactivateProgressAnimation();
			send.setEnabled(true);
			break;
		}
	}

	@Override
	public void onPostStateChange(SimpleChatWithPictureHash post, IChatroom.PostState state, ChatEntry chat) {
		switch (state) {
			case Success:
				text.getText().clear();
				loading.setVisibility(View.GONE);
				chatAdapter.changeCursor(chatroom.getChatCursor());
				chosen_picture.setVisibility(View.GONE);
				pictureHandler.discardPicture();
				break;
			case Failure:
				loading.setVisibility(View.GONE);
				Toast.makeText(getActivity(), getString(R.string.chat_post_failure), Toast.LENGTH_SHORT).show();
				chosen_picture.setVisibility(View.GONE);
				pictureHandler.discardPicture();
				break;
			case Uploading:
				loading.setVisibility(View.VISIBLE);
				break;
		}
	}

	@Override
	public void onClick(View v) {
		Editable msg = text.getText();
        IPicture pic = pictureHandler.getPicture();
		String hash = null;
        if (pic != null) {
			hash = pic.getHash();
		}
        if (pic != null || msg.length() > 0 ) {
			chatroom.postChat(msg.toString(), hash, null);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		Integer picID = chatAdapter.getPictureID(pos);
		if (picID == null)
			return;

		Intent intent = new Intent(getActivity(), PictureGalleryActivity.class);
		intent.putExtra(Std.PICTURE_GALLERY, chatroom.getPictureGallery(picID));
//				intent.putExtra(Std.CHATROOM, chatroom.getID());
//				intent.putExtra(Std.IMAGE, picID);
		startActivity(intent);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE) {
			int lastPos = view.getLastVisiblePosition();
			chatroom.setLastReadId(chatAdapter.getChatID(lastPos));
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

	}

	public static ChatroomFragment newInstance(int chatroomId) {
		ChatroomFragment f = new ChatroomFragment();
		Bundle b = new Bundle();
		b.putInt(Std.CHATROOM, chatroomId);
		f.setArguments(b);

		return f;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Server.getCurrentServer().releaseChatManager(this);

	}

	@Override
	public Handler getCallbackHandler() {
		return Threading.getUiExecutor();
	}
}
