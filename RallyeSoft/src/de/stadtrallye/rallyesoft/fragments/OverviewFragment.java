package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;

public class OverviewFragment extends SherlockFragment implements IModel.IModelListener {

	@SuppressWarnings("unused")
	private static final String THIS = OverviewFragment.class.getSimpleName();

	private IModel model;
	private TextView connectionState;
	private TextView serverDesc;
	private TextView serverName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.overview_fragment, container, false);

		connectionState = (TextView) v.findViewById(R.id.server_status);
		serverName = (TextView) v.findViewById(R.id.server_name);
		serverDesc = (TextView) v.findViewById(R.id.server_desc);

		return v;
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

		onConnectionStateChange(model.getConnectionState());
		if (model.getServerInfo() != null)
			onServerInfoChange(model.getServerInfo());
		model.addListener(this);

	}

	@Override
	public void onStop() {
		super.onStop();
		model.removeListener(this);
	}

	@Override
	public void onConnectionStateChange(IModel.ConnectionState newState) {
		connectionState.setText((newState == IModel.ConnectionState.Connected) ? R.string.connected : R.string.notConnected);
	}

	@Override
	public void onConnectionFailed(Exception e, IModel.ConnectionState fallbackState) {
		connectionState.setText(e.toString());
	}

	@Override
	public void onServerConfigChange() {

	}

	@Override
	public void onServerInfoChange(ServerInfo info) {
		serverName.setText(info.name);
		serverDesc.setText(info.description +"\n"+ info.getApiAsString());
	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {

	}
}
