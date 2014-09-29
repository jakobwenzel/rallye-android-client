/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
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
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IServer;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.threading.Threading;

public class OverviewFragment extends Fragment implements IServer.IServerListener {

	@SuppressWarnings("unused")
	private static final String THIS = OverviewFragment.class.getSimpleName();

	private TextView connectionState;
	private TextView serverDesc;
	private TextView serverName;
	private TextView serverVer;
	private Server server;

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

		server = Server.getCurrentServer();

		showServerInfo(server.getServerInfoCached());

		server.addListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		server.removeListener(this);
		server = null;
	}

	private void showServerInfo(ServerInfo info) {
		if (info != null) {
			serverName.setText(info.name);
			serverDesc.setText(info.description);
			StringBuilder sb = new StringBuilder();
			for (ServerInfo.Api api : info.api) {
				sb.append(api.name).append(": ").append(api.version).append('\n');
			}
			sb.deleteCharAt(sb.length() - 1);
			serverVer.setText(sb.toString());
		}
	}

	private void hideServerInfo() {
		serverName.setText("");
		serverDesc.setText("");
		serverVer.setText("");
	}

//	@Override
//	public void onConnectionStateChange(IModel.ConnectionState newState) {
//		connectionState.setText(newState.toString());
//
//		if (newState == IModel.ConnectionState.Connected) {
//			showServerInfo(server.getServerInfo());
//		}
//	}

	@Override
	public void onLoginSuccessful() {

	}

	@Override
	public void onConnectionFailed(Exception e, int status) {
		connectionState.setText(e.toString());
		hideServerInfo();
	}

	@Override
	public void onServerInfoChanged(ServerInfo info) {
		showServerInfo(info);
	}

	@Override
	public void onAvailableGroupsChanged(List<Group> groups) {

	}

	@Override
	public Handler getCallbackHandler() {
		return Threading.getUiExecutor();
	}
}
