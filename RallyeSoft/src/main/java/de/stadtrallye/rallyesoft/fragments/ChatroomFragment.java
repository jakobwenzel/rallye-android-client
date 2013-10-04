package de.stadtrallye.rallyesoft.fragments;

import android.content.Intent;
import android.os.Bundle;
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

import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.ImageLoader;

import de.stadtrallye.rallyesoft.PictureGalleryActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.uimodel.ChatCursorAdapter;
import de.stadtrallye.rallyesoft.uimodel.IPictureTakenListener;
import de.stadtrallye.rallyesoft.uimodel.IProgressUI;

import static de.stadtrallye.rallyesoft.model.Model.getModel;

/**
 * One Chatroom, with input methods
 * @author Ramon
 *
 */
public class ChatroomFragment extends SherlockFragment implements IChatroom.IChatroomListener, OnClickListener, OnItemClickListener, AbsListView.OnScrollListener {

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

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			ui = (IProgressUI) getActivity();
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

		IModel model = getModel(getActivity());
		chatroom = model.getChatroom(roomID);

		if (chatroom == null) {
			throw new IllegalArgumentException(THIS +" could not find the Model of Chatroom "+ roomID);
		}

		chatAdapter = new ChatCursorAdapter(getActivity(), model, chatroom.getChatCursor());
		list.setAdapter(chatAdapter);
		list.setOnScrollListener(this);//TODO: use chatroom.getLastReadId() (wrap cursorAdapter add extra line)

		parent = (IPictureTakenListener) getParentFragment();
		IPictureTakenListener.Picture pic = parent.getPicture();
		if (pic != null) {
			chosen_picture.setVisibility(View.VISIBLE);
			ImageLoader.getInstance().displayImage("file://"+ pic.getPath(), chosen_picture);
		} else {
			chosen_picture.setVisibility(View.GONE);
		}

//        restoreScrollState();
		
		chatroom.addListener(this);
		chatAdapter.changeCursor(chatroom.getChatCursor());
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
//		saveScrollState();
		
		chatroom.setLastReadId(lastPos[0]);
		
		chatroom.removeListener(this);
	}
	
	private void saveScrollState() {
		lastPos[0] = list.getFirstVisiblePosition(); 
		
		if (lastPos[0] == 0) {
			lastPos[1] = 0;
		} else {
			View v = list.getChildAt(0);
			lastPos[1] = v.getTop(); 
		}
	}
	
	private void restoreScrollState() {
		if (lastPos != null) {
	    	list.setSelectionFromTop(lastPos[0], lastPos[1]);
	    	if (DEBUG)
				Log.v(THIS, "ScrollState restored: "+ lastPos[0]);
        } else
        	list.setSelection(list.getCount()-1);
	}
	
	/**
	 * Save the current scroll position
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		saveScrollState();
		
		if (DEBUG)
			Log.v(THIS, "ScrollState saved: "+ lastPos[0]);
		
		outState.putSerializable(Std.LAST_POS, lastPos);
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
		if (msg.length() > 0) {
			IPictureTakenListener.Picture pic = parent.getPicture();
			if (pic != null) {
				chatroom.postChatWithHash(msg.toString(), pic.getHash());
				loading.setVisibility(View.VISIBLE);
			} else {
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
