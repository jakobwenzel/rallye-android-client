package de.stadtrallye.rallyesoft.fragments;

import com.actionbarsherlock.app.SherlockFragment;

import de.stadtrallye.rallyesoft.common.Std;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DummyFragment extends SherlockFragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(false);
	}

	public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(getArguments().getInt(Std.LAYOUT), container, false);
	}
}
