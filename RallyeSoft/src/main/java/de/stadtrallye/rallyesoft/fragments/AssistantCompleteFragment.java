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

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13
 */
public class AssistantCompleteFragment extends Fragment implements View.OnClickListener, IModel.IModelListener {

	private IConnectionAssistant assistant;
	private Button next;
	private Button cancel;
	private boolean started = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_complete_fragment, container, false);
		next = (Button) v.findViewById(R.id.next);
		next.setOnClickListener(this);

		cancel = (Button) v.findViewById(R.id.cancel);
		cancel.setOnClickListener(this);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			assistant = (IConnectionAssistant) getActivity();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IConnectionAssistant");
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		if (!started) {
			assistant.getModel().addListener(this);
			assistant.login();
			started=true;
		}

	}

//	@Override
//	public void onStop() {
//		super.onStop();
//
//	}

	@Override
	public void onClick(View v) {
		assistant.getModel().removeListener(this);
		if (v == next)
			assistant.finish(true);
		else
			assistant.finish(false);
	}

	@Override
	public void onConnectionStateChange(IModel.ConnectionState newState) {
		if (newState == IModel.ConnectionState.Connected) {
			next.setEnabled(true);
		}
	}

	@Override
	public void onConnectionFailed(Exception e, IModel.ConnectionState fallbackState) {
		Toast.makeText(getActivity(), R.string.invalid_login, Toast.LENGTH_SHORT).show();
		assistant.back();
	}

	@Override
	public void onServerInfoChange(ServerInfo info) {

	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {

	}
}
