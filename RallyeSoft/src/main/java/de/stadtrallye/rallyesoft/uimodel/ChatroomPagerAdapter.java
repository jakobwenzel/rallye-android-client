package de.stadtrallye.rallyesoft.uimodel;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.util.List;

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

	public ChatroomFragment getCurrentFragment() {
		return current;
	}

	/**
	 * Needed so the FragmentManager can distinguish tabs of different chatrooms (, if re logging in as different user)
	 * Default behavior, will name Fragments after their position
	 */
	@Override
	public long getItemId(int position) {
		return chatrooms.get(position).getID();
	}

	@Override
	public Fragment getItem(int pos) {
		ChatroomFragment f;

		f = new ChatroomFragment();
		Bundle b = new Bundle();
		b.putInt(Std.CHATROOM, chatrooms.get(pos).getID());
		f.setArguments(b);

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
