/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;

import static de.stadtrallye.rallyesoft.model.Model.getModel;

public class OverviewFragment extends Fragment implements IModel.IModelListener {

	@SuppressWarnings("unused")
	private static final String THIS = OverviewFragment.class.getSimpleName();

	private TextView connectionState;
	private TextView serverDesc;
	private TextView serverName;
	private TextView serverVer;

	private IModel model;

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
		serverVer = (TextView) v.findViewById(R.id.server_ver);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();

		model = getModel(getActivity());

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

	private void showServerInfo(ServerInfo info) {
		serverName.setText(info.name);
		serverDesc.setText(info.description);
		StringBuilder sb = new StringBuilder();
		for (ServerInfo.Api api: info.api) {
			sb.append(api.name).append(": ").append(api.version).append('\n');
		}
		sb.deleteCharAt(sb.length()-1);
		serverVer.setText(sb.toString());
	}

	private void hideServerInfo() {
		serverName.setText("");
		serverDesc.setText("");
		serverVer.setText("");
	}

	@Override
	public void onConnectionStateChange(IModel.ConnectionState newState) {
		connectionState.setText(newState.toString());

		if (newState == IModel.ConnectionState.Connected) {
			showServerInfo(model.getServerInfo());
		}
	}

	@Override
	public void onConnectionFailed(Exception e, IModel.ConnectionState fallbackState) {
		connectionState.setText(fallbackState +"\n"+ e.toString());
		hideServerInfo();
	}

	@Override
	public void onServerInfoChange(ServerInfo info) {
		showServerInfo(info);
	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {

	}
}
