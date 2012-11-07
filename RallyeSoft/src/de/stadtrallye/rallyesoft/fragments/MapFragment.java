package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.async.PullTest;
import de.stadtrallye.rallyesoft.communications.Pull;

public class MapFragment extends Fragment {
	
	PullTest t;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		t = new PullTest(this, new Pull(Config.server, false));
//		t.execute();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.map, container, false);
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
}
