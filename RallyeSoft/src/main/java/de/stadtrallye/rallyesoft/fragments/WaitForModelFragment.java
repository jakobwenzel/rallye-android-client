package de.stadtrallye.rallyesoft.fragments;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.stadtrallye.rallyesoft.R;

/**
 * Placeholder that can be shown whenever there is no other applicable fragment
 * Specifically during loading and closing the ConnectionAssistant, to force all other fragments that depend on Model to refresh
 */
public class WaitForModelFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.wait_for_model, container, false);

		return v;
	}
}
