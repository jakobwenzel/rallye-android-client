package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.MainActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.async.PullChats;
import de.stadtrallye.rallyesoft.communications.Pull;
import de.stadtrallye.rallyesoft.communications.RallyePull;

public class ChatFragment extends Fragment {
	
	private RallyePull pull;
	
	public ChatFragment(RallyePull pull) {
		this.pull = pull;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PullChats c = new PullChats(this, pull);
		c.execute();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.chat_list, container, false);
	}
}
