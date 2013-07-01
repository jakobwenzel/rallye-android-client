package de.stadtrallye.rallyesoft.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.List;

import de.stadtrallye.rallyesoft.ImageViewActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.uimodel.ChatAdapter;
import de.stadtrallye.rallyesoft.uimodel.ChatCursorAdapter;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.IProgressUI;

/**
 * One Chatroom, with input methods
 * @author Ramon
 *
 */
public class ChatroomFragment extends SherlockFragment implements IChatroom.IChatroomListener, OnClickListener {

	private static final String THIS = ChatroomFragment.class.getSimpleName();
	private static final boolean DEBUG = false;
	
	
	private IModel model;
	private int[] lastPos = null; //[0] = line, [1] = px
	private IChatroom chatroom;
	
	private IProgressUI ui; // access to IndeterminateProgress in ActionBar
	
	private ListView list;	//List of ChatEntries
	private ChatCursorAdapter chatAdapter; //Adapter for List
	private Button send;
	private EditText text;
	private ProgressBar loading;

	
	/**
	 * retain savedInstanceState for when creating the list (ScrollState)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
			lastPos = savedInstanceState.getIntArray(Std.LAST_POS);
		else
			lastPos = new int[2];
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.chat_list, container, false);
		list = (ListView) v.findViewById(R.id.chat_list);
		send = (Button) v.findViewById(R.id.button_send);
		text = (EditText) v.findViewById(R.id.edit_new_message);
		loading = (ProgressBar) v.findViewById(R.id.loading);

		send.setOnClickListener(this);

		return v;
	}
	
	/**
	 * get Model and method to show Progress from Activity
	 * needs Activity to implement {@link IProgressUI} and {@link IModelActivity}
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		try {
			model = ((IModelActivity) getActivity()).getModel();
			ui = (IProgressUI) getActivity();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity");
		}
		
		Bundle args = getArguments();
		int roomId = -1;
		if (args != null)
			roomId = args.getInt(Std.CHATROOM, -1);
		
		chatroom = model.getChatroom(roomId);
		
		if (chatroom == null) {
			throw new UnsupportedOperationException(THIS +" could not find the Model of Chatroom "+ savedInstanceState.getInt(Std.CHATROOM));
		}
		
//		chatAdapter = new ChatAdapter(getActivity(), model);
//        list.setAdapter(chatAdapter);
		chatAdapter = new ChatCursorAdapter(getActivity(), chatroom.getChatCursor(), model);
        
        restoreScrollState();
        
        list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				Integer picID = chatAdapter.getPictureID(pos);
				if (picID == null)
					return;
				
				Intent intent = new Intent(getActivity(), ImageViewActivity.class);
				intent.putExtra(Std.CHATROOM, chatroom.getID());
				intent.putExtra(Std.IMAGE, chatAdapter.getPictureID(pos));
				startActivity(intent);
			}
		});
	}
	
	/**
	 * if we are logged in show progress and start asynchronous chat refresh
	 */
	@Override
	public void onStart() {
		super.onStart();
		
		chatroom.addListener(this);
		chatAdapter.changeCursor(chatroom.getChatCursor());
		chatroom.provideChats(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		saveScrollState();
		
		chatroom.saveCurrentState(lastPos[0]);
		
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
	public void chatsEdited(List<ChatEntry> chats) {
//		chatAdapter.editChats(chats);
	}

	@Override
	public void chatsAdded(List<ChatEntry> chats) {
//		chatAdapter.addChats(chats);
	}

	@Override
	public void chatsProvided(List<ChatEntry> chats) {
//		chatAdapter.replaceChats(chats);
	}
	
	@Override
	public void onChatStatusChanged(IChatroom.ChatroomState newStatus) {
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
	public void onPostStateChange(int id, IChatroom.PostState state, ChatEntry chatEntry) {
		switch (state) {
			case Success:
				text.getText().clear();
				loading.setVisibility(View.GONE);
//				chatAdapter.addChat(chatEntry);
				break;
			case Failure:
				loading.setVisibility(View.GONE);
				Toast.makeText(getActivity(), getString(R.string.chat_post_failure), Toast.LENGTH_SHORT).show();
				break;
			case Retrying:
				break;
		}
	}

	@Override
	public void onClick(View v) {
		Editable msg = text.getText();
		if (msg.length() > 0) {
			chatroom.postChat(msg.toString(), null);
			loading.setVisibility(View.VISIBLE);
		}
	}
}
