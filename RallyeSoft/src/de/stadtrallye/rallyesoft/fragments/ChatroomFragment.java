package de.stadtrallye.rallyesoft.fragments;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import de.stadtrallye.rallyesoft.IModelActivity;
import de.stadtrallye.rallyesoft.IProgressUI;
import de.stadtrallye.rallyesoft.ImageViewActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.Std;
import de.stadtrallye.rallyesoft.model.ChatEntry;
import de.stadtrallye.rallyesoft.model.IModelResult;
import de.stadtrallye.rallyesoft.model.Model;

/**
 * One Chatroom, with input methods
 * @author Ramon
 *
 */
public class ChatroomFragment extends BaseFragment implements IModelResult<List<ChatEntry>> {
	
	final static private int TASK_CHAT = 101;

	private Model model;
	private IProgressUI ui;
	private ListView list;
//	private ImageView img;
	private int[] lastPos = null;
	private int chatroom;
	
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
		
		if (savedInstanceState != null)
			lastPos = savedInstanceState.getIntArray(Std.LAST_POS);
		
		chatroom = getArguments().getInt("chatroom");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.chat_list, container, false);
		list = (ListView)v.findViewById(R.id.chat_list);
//		img = (ImageView)v.findViewById(R.id.chat_backside);
//		img.setRotationY(-90f);
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
			model.retrieveCompleteChat(this, TASK_CHAT, chatroom);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		restoreScrollState();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (lastPos == null)
			lastPos = new int[2];
		lastPos[0] = list.getFirstVisiblePosition(); 
		
		View v = list.getChildAt(0);
		lastPos[1] = v.getTop(); 
	}
	
	/**
	 * Save the current scroll position (not yet pixel accurate)
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if (DEBUG)
			Log.v("ChatFragment", "ScrollState: "+ lastPos);
		
		outState.putIntArray(Std.LAST_POS, lastPos);
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
		
        final ChatAdapter chatAdapter = new ChatAdapter(getActivity(), R.layout.chat_item, result);
        list.setAdapter(chatAdapter);
        
        restoreScrollState();
        
        list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				if (!chatAdapter.hasPicture(pos))
					return;
				
				Intent intent = new Intent(getActivity(), ImageViewActivity.class);
				intent.putExtra(Std.CHATROOM, chatroom);
				intent.putExtra(Std.IMAGE_LIST, chatAdapter.getPictures());
				intent.putExtra(Std.IMAGE, chatAdapter.getPicturePos(pos));
				startActivity(intent);
			}
		});
        
        ui.deactivateProgressAnimation();
	}
	
//	private void flipit() {
//        final ListView visibleList;
//        final ListView invisibleList;
//        if (mEnglishList.getVisibility() == View.GONE) {
//            visibleList = mFrenchList;
//            invisibleList = mEnglishList;
//        } else {
//            invisibleList = mFrenchList;
//            visibleList = mEnglishList;
//        }
//        ObjectAnimator visToInvis = ObjectAnimator.ofFloat(visibleList, "rotationY", 0f, 90f);
//        visToInvis.setDuration(500);
//        visToInvis.setInterpolator(accelerator);
//        final ObjectAnimator invisToVis = ObjectAnimator.ofFloat(invisibleList, "rotationY",
//                -90f, 0f);
//        invisToVis.setDuration(500);
//        invisToVis.setInterpolator(decelerator);
//        visToInvis.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator anim) {
//                visibleList.setVisibility(View.GONE);
//                invisToVis.start();
//                invisibleList.setVisibility(View.VISIBLE);
//            }
//        });
//        visToInvis.start();
//    }
	
	private void restoreScrollState() {
		if (lastPos != null) {
	    	list.setSelectionFromTop(lastPos[0], lastPos[1]);
	    	if (DEBUG)
				Log.v("ChatFragment", "ScrollState restored: "+ lastPos);
        } else
        	list.setSelection(list.getCount()-1);
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
		private int[] pictures;

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
			final ChatEntry o = entries.get(position);
			
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
                mem.time.setText(converter.format(new Date(o.timestamp * 1000L)));
                
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
		
		public boolean hasPicture(int pos) {
			return entries.get(pos).pictureID != 0;
		}
		
		public int[] getPictures() {
			if (pictures != null)
				return pictures;
			
			ArrayList<Integer> l = new ArrayList<Integer>();
			
			for (ChatEntry c: entries) {
				if (c.pictureID > 0)
					l.add(c.pictureID);
			}
			
			pictures = new int[l.size()];
			for (int i=l.size()-1;i>=0;--i)
				pictures[i] = l.get(i);
			
			return pictures;
		}
		
		public int getPicturePos(int pos) {
			final int pictureID = entries.get(pos).pictureID;
			
			for (int i=pictures.length-1; i>=0; --i) {
				if (pictures[i] == pictureID)
					return i;
			}
			return -1;
		}
	}
}
