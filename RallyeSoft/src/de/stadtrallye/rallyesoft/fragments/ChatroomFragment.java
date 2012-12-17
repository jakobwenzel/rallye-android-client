package de.stadtrallye.rallyesoft.fragments;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import de.stadtrallye.rallyesoft.IModelActivity;
import de.stadtrallye.rallyesoft.IProgressUI;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.ChatEntry;
import de.stadtrallye.rallyesoft.model.IModelResult;
import de.stadtrallye.rallyesoft.model.Model;

/**
 * One Chatroom, with input methods
 * @author Ramon
 *
 */
public class ChatroomFragment extends BaseFragment implements IModelResult<List<ChatEntry>> {
	
	private static final String LAST_POS = "lastPosition"; 
	
	final static private int TASK_CHAT = 101;

	private Model model;
	private IProgressUI ui;
	private ListView list;
	private Bundle restore;
	
	/**
	 * Only for DEBUG purposes
	 */
	public ChatroomFragment() {
		
		THIS = ChatroomFragment.class.getSimpleName();
		
		if (DEBUG)
			Log.v(THIS, "Instantiated "+ this.toString());
	}
	
	/**
	 * retain saavedInstanceState for when creating the list (ScrollState)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		restore = savedInstanceState;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.chat_list, container, false);
		list = (ListView)v.findViewById(R.id.chat_list);
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
	}
	
	/**
	 * if we are logged in show progress and start asynchronous chat refresh
	 */
	@Override
	public void onStart() {
		super.onStart();
		
		if (model.isLoggedIn()) {
			ui.activateProgressAnimation();
			if (DEBUG)
				Log.v(THIS, "Model call from "+ this.toString() +" , Activity: "+ getActivity());
			model.refreshSimpleChat(this, TASK_CHAT, getArguments().getInt("chatroom"));
		}
	}
	
	/**
	 * Save the current scroll position (not yet pixel accurate)
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if (DEBUG)
			Log.v("ChatFragment", "ScrollState: "+ list.getFirstVisiblePosition());
		
		outState.putInt(LAST_POS, list.getFirstVisiblePosition());
	}
	
	
	/**
	 * Callback from Model, expecting:
	 * - tag:TASK_CHAT
	 */
	@Override
	public void onModelFinished(int tag, List<ChatEntry> result) {
		
		if (tag != TASK_CHAT)
			return;
		
		// Only DEBUG, activity should never be null.
		// turns out i tried to forcefully re use an already destroyed fragment, because my FragmentHandler remembered it
		if (getActivity() != null)
		{
			if (DEBUG)
				Log.v(THIS, "Model callback to "+ this.toString() +" , Activity: "+ getActivity());
		} else {
			if (DEBUG)
				Log.e(THIS, "Model callback to "+ this.toString() +" , Activity: null");
			return;
		}
		
        ChatAdapter chatAdapter = new ChatAdapter(getActivity(), R.layout.chat_item, result);
        list.setAdapter(chatAdapter);
        if (restore != null) {
        	list.setSelectionFromTop(restore.getInt(LAST_POS, 0), 0);
        	if (DEBUG)
    			Log.v("ChatFragment", "ScrollState restored: "+ restore.getInt(LAST_POS, 0));
        }
        
        ui.deactivateProgressAnimation();
	}
	
	/**
	 * Wraps around ChatEntry List
	 * Uses R.layout.chat_item / R.layout.chat_item_right, depending on $Chatentry.self
	 * @author Ramon
	 *
	 */
	private class ChatAdapter extends ArrayAdapter<ChatEntry> {

		private List<ChatEntry> entries;
		private ImageLoader loader;
		private DateFormat converter;

		public ChatAdapter(Context context, int textViewResourceId, List<ChatEntry> entries) {
			super(context, textViewResourceId, entries);
			this.entries = entries;
			
			loader = ImageLoader.getInstance();
			DisplayImageOptions disp = new DisplayImageOptions.Builder()
				.cacheOnDisc()
				.cacheInMemory() // Still unlimited Chache on Disk
				.showStubImage(R.drawable.stub_image)
				.build();
			ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getContext())
//				.enableLogging()
				.defaultDisplayImageOptions(disp)
				.build();
            loader.init(config);
            
            converter = SimpleDateFormat.getDateTimeInstance();
		}
		
		public class ViewMem {
			public ImageView img;
			public TextView msg;
			public TextView sender;
			public TextView time;
		}
		
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			ChatEntry o = entries.get(position);
			
			ViewMem mem;
			
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate((o.self)? R.layout.chat_item_right : R.layout.chat_item, null);
                
                mem = new ViewMem();
                
                mem.img = (ImageView) v.findViewById(R.id.sender_img);
                mem.sender = (TextView) v.findViewById(R.id.msg_sender);
                mem.msg = (TextView) v.findViewById(R.id.msg);
                mem.time = (TextView) v.findViewById(R.id.time_sent);
                
                v.setTag(mem);
                
            } else {
            	mem = (ViewMem) v.getTag();
            }
            
            if (o != null) {
                mem.sender.setText("Sender: "+ o.senderID);
                mem.msg.setText(o.message);
                mem.time.setText(converter.format(new Date(o.timestamp)));
                
                // ImageLoader jar
                // ImageLoader must apparently be called for _EVERY_ entry
                // When called with null or "" as URL, will display empty pciture / default resource
                // Otherwise ImageLoader will not be stable and start swapping images
                if (o.pictureID > 0) {
                	loader.displayImage(model.getImageUrl(o.pictureID, 't'), mem.img);
                } else {
                	loader.displayImage(null, mem.img);
                }
                
//                Log.v("ChatAdapter", "["+o.timestamp+"] '"+ o.message +"' (pic:"+ o.pictureID +")");
            }
            return v;
		}
	}
}
