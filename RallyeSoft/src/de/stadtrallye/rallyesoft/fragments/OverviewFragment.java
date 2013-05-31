package de.stadtrallye.rallyesoft.fragments;

import com.actionbarsherlock.app.SherlockFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IConnectionStatusListener;
import de.stadtrallye.rallyesoft.model.IModel.ConnectionStatus;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;

public class OverviewFragment extends SherlockFragment implements IConnectionStatusListener {
	
	@SuppressWarnings("unused")
	private static final String THIS = OverviewFragment.class.getSimpleName();
	
	private Model model;
	private TextView connectionStatus;

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

		onConnectionStatusChange(model.getConnectionStatus());
		model.addListener(this);
		
	}
	
	@Override
	public void onStop() {
		super.onStop();
		model.removeListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.overview_fragment, container, false);
		connectionStatus = (TextView) v.findViewById(R.id.server_status);
		return v;
	}

	@Override
	public void onConnectionStatusChange(ConnectionStatus newStatus) {
		connectionStatus.setText((newStatus == ConnectionStatus.Connected)? R.string.connected : R.string.notConnected);
	}

	@Override
	public void onConnectionFailed(Exception e, ConnectionStatus lastStatus) {
		connectionStatus.setText(e.getMessage());
	}
}
