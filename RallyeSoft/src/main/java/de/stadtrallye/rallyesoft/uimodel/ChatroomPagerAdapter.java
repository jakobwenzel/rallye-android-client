package de.stadtrallye.rallyesoft.uimodel;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.fragments.ChatroomFragment;
import de.stadtrallye.rallyesoft.model.IChatroom;

/**
 * Adapter for a List<IChatroom>
 */
public class ChatroomPagerAdapter extends FragmentPagerAdapter {

	private List<? extends IChatroom> chatrooms;
	private ChatroomFragment current;

	public ChatroomPagerAdapter(FragmentManager fm, List<? extends IChatroom> chatrooms) {
		super(fm);
		this.chatrooms = chatrooms;
	}

// --Commented out by Inspection START (22.09.13 02:46):
//	public ChatroomFragment getCurrentFragment() {
//		return current;
//	}
// --Commented out by Inspection STOP (22.09.13 02:46)

	/**
	 * Needed so the FragmentManager can distinguish tabs of different chatrooms (, if re logging in as different user)
	 * Default behavior, will name Fragments after their position
	 */
	@Override
	public long getItemId(int position) {
		return chatrooms.get(position).getID();
	}

	Map<Integer,ChatroomFragment> fragments = new HashMap<Integer,ChatroomFragment>();

	@Override
	public ChatroomFragment getItem(int pos) {
		ChatroomFragment f = fragments.get(pos);
		if (f!=null) return f;

		f = new ChatroomFragment();
		Bundle b = new Bundle();
		b.putInt(Std.CHATROOM, chatrooms.get(pos).getID());
		f.setArguments(b);
		fragments.put(pos,f);

		return f;
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object) {
		super.setPrimaryItem(container, position, object);

		current = (ChatroomFragment) object;
	}

	@Override
	public int getCount() {
		return (chatrooms != null)? chatrooms.size() : 0;
	}

	@Override
	public CharSequence getPageTitle(int pos) {
		return chatrooms.get(pos).getName();
	}
}
