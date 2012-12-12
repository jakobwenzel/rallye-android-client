package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

public class DummyFragment extends SherlockFragment {

	public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(getArguments().getInt("layout"), container, false);
	}
}
