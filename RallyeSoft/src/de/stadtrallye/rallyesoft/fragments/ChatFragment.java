package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.async.PullChats;

public class ChatFragment extends SherlockFragment {
	
	public ChatFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PullChats c = new PullChats(this);
		c.execute();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.chat_list, container, false);
	}
}
