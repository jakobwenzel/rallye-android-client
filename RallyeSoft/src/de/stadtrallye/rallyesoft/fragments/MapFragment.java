package de.stadtrallye.rallyesoft.fragments;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.communications.Pull;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MapFragment extends Fragment {
	
	Pull pull = new Pull();
	String text;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		text = pull.testConnection();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.map_fragment, container, false);
	}
	
	@Override
	public void onStart() {
		TextView tv = (TextView) (getView().findViewById(R.id.placeholder));
		tv.setText(text);
	}
}
