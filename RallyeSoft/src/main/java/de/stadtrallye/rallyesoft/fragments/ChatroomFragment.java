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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
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

import de.stadtrallye.rallyesoft.PictureGalleryActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.uimodel.ChatCursorAdapter;
import de.stadtrallye.rallyesoft.uimodel.IPicture;
import de.stadtrallye.rallyesoft.uimodel.IPictureTakenListener;
import de.stadtrallye.rallyesoft.uimodel.IProgressUI;

import static de.stadtrallye.rallyesoft.model.Model.getModel;

/**
 * One Chatroom, with input methods
 * @author Ramon
 *
 */
public class ChatroomFragment extends Fragment implements IChatroom.IChatroomListener, OnClickListener, OnItemClickListener, AbsListView.OnScrollListener {

	private static final String THIS = ChatroomFragment.class.getSimpleName();
	private static final boolean DEBUG = false;
	

	private int roomID;
//	private IModel model;
	private int[] lastPos = null; //[0] = line, [1] = px
	private IChatroom chatroom;
	
	private IProgressUI ui; // access to IndeterminateProgress in ActionBar
	
	private ListView list;	//List of ChatEntries
	private ChatCursorAdapter chatAdapter; //Adapter for List
	private ImageButton send;
	private EditText text;
	private ProgressBar loading;
	private ImageView chosen_picture;
	private IPictureTakenListener parent;
	private IModel model;


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
		model = getModel(getActivity());
		chatroom = model.getChatroom(roomID);

		if (chatroom == null) {
			throw new IllegalArgumentException(THIS +" could not find the Model of Chatroom "+ roomID);
		}

		chatAdapter = new ChatCursorAdapter(getActivity(), model, chatroom);
		list.setAdapter(chatAdapter);
		list.setOnScrollListener(this);//TODO: use chatroom.getLastReadId() (wrap cursorAdapter add extra line)

		loadImagePreview();

		chatroom.addListener(this);
		chatAdapter.changeCursor(chatroom.getChatCursor());

		restoreLastReadId(chatroom.getLastReadId());
	}

	public void loadImagePreview() {
		parent = (IPictureTakenListener) getParentFragment();
		IPicture pic = parent.getPicture();
		if (pic != null) {
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

			loadChats();

		} catch (ClassCastException e) {
			Log.i(THIS, "Activity must implement IProgressUI");
			throw e;
		}
	}

	/**
	 * if we are logged in show progress and start asynchronous chat refresh
	 */
	@Override
	public void onStart() {
		super.onStart();

		if (model!=getModel(getActivity()))
			loadChats();
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
	public void onChatroomStateChanged(IChatroom.ChatroomState newStatus) {
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
	public void onPostStateChange(int id, IChatroom.PostState state, ChatEntry chat) {
		switch (state) {
			case Success:
				text.getText().clear();
				loading.setVisibility(View.GONE);
				chatAdapter.changeCursor(chatroom.getChatCursor());
				chosen_picture.setVisibility(View.GONE);
				parent.sentPicture();
				break;
			case Failure:
				loading.setVisibility(View.GONE);
				Toast.makeText(getActivity(), getString(R.string.chat_post_failure), Toast.LENGTH_SHORT).show();
				chosen_picture.setVisibility(View.GONE);
				parent.sentPicture();
				break;
			case Retrying:
				break;
		}
	}

	@Override
	public void onClick(View v) {
		Editable msg = text.getText();
        IPicture pic = parent.getPicture();
        if (pic != null) {
            chatroom.postChatWithHash(msg.toString(), pic.getHash());
            loading.setVisibility(View.VISIBLE);
        } else if (msg.length() > 0 ) {{
            chatroom.postChat(msg.toString(), null);
        }
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
}
