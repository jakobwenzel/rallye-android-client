package de.stadtrallye.rallyesoft.uimodel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry.Sender;
import de.stadtrallye.rallyesoft.model.structures.GroupUser;

/**
 * Wraps around ChatEntry List
 * Uses R.layout.chat_item / R.layout.chat_item_right, depending on $Chatentry.self
 * @author Ramon
 *
 */
public class ChatAdapter extends BaseAdapter {

	private List<ChatEntry> chats = new ArrayList<ChatEntry>();
	private ImageLoader loader;
	private DateFormat converter;
	private GroupUser user;
	private Context context;
	private LayoutInflater inflator;
	private IChatroom chatroom;
	
	private class ViewMem {
		public ImageView img;
		public TextView msg;
		public TextView sender;
		public TextView time;
	}
	
	
	public ChatAdapter(Context context, GroupUser user, IChatroom chatroom) {
		this.context = context;
		this.user = user;
		this.chatroom = chatroom;
		
		this.inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		loader = ImageLoader.getInstance();
		DisplayImageOptions disp = new DisplayImageOptions.Builder()
			.cacheOnDisc()
			.cacheInMemory() //TODO: Still unlimited Cache on Disk
			.showStubImage(R.drawable.stub_image)
			.build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
			.defaultDisplayImageOptions(disp)
			.build();
        loader.init(config);
        
        converter = SimpleDateFormat.getDateTimeInstance();
	}
	
	
	public ChatEntry getChatEntry(int pos) {
		return chats.get(pos);
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		final ChatEntry o = chats.get(position);
		
		ViewMem mem;
		
        if (v == null) {
            Sender s = o.getSender(user);
            
            v = inflator.inflate((s == Sender.Me)? R.layout.chat_item_right : R.layout.chat_item_left, null);
            
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
            mem.sender.setText("Sender: "+ o.userID);
            mem.msg.setText(o.message);
            mem.time.setText(converter.format(new Date(o.timestamp * 1000L)));
            
            // ImageLoader jar
            // ImageLoader must apparently be called for _EVERY_ entry
            // When called with null or "" as URL, will display empty pciture / default resource
            // Otherwise ImageLoader will not be stable and start swapping images
            if (o.pictureID > 0) {
            	loader.displayImage(chatroom.getUrlFromImageId(o.pictureID, 't'), mem.img);
            } else {
            	loader.displayImage(null, mem.img);
            }
            
        }
        
        return v;
	}
	
	public boolean hasPicture(int pos) {
		return chats.get(pos).pictureID != 0;
	}

	@Override
	public int getCount() {
		return chats.size();
	}

	@Override
	public ChatEntry getItem(int position) {
		return chats.get(position);
	}


	@Override
	public long getItemId(int position) {
		return chats.get(position).chatID;
	}
	
	public void updateChats(List<ChatEntry> chats) {
		this.chats = chats;
		notifyDataSetChanged();
	}
	
	public void addChats(List<ChatEntry> chats) {
		this.chats.addAll(chats);
		notifyDataSetChanged();
	}
}
