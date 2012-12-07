package de.stadtrallye.rallyesoft.fragments;

import de.stadtrallye.rallyesoft.IModelActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModelResult;
import de.stadtrallye.rallyesoft.model.Model;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class OverviewFragment extends Fragment implements IModelResult<Boolean> {
	
	private static final int TASK_CHECK_STATUS = 201;
	private Model model;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		try {
			model = ((IModelActivity) getActivity()).getModel();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity");
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		TextView t = (TextView) getView().findViewById(R.id.server_status);
		t.setText((model.isLoggedIn())? R.string.connected : R.string.notConnected);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.overview_fragment, container, false);
	}

	@Override
	public void onModelFinished(int tag, Boolean result) {
		if (tag != TASK_CHECK_STATUS)
			return;
		
		getActivity().setProgressBarIndeterminateVisibility(false);
	}
}
