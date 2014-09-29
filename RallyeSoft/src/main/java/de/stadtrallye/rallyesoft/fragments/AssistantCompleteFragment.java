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
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.LoginInfo;
import de.rallye.model.structures.PushConfig;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IServer;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.threading.Threading;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;
import de.stadtrallye.rallyesoft.uimodel.Util;
import de.wirsch.gcm.GcmHelper;

/**
 * Created by Ramon on 19.06.13
 */
public class AssistantCompleteFragment extends Fragment implements View.OnClickListener, IServer.IServerListener, AssistantPasswordDialogFragment.IPasswordRetry {

	private static final String THIS = AssistantCompleteFragment.class.getSimpleName();

	private IConnectionAssistant assistant;
	private Button btn_next;
	private Button btn_cancel;
	private boolean started = false;
	private TextView text_status;
	private ProgressBar prg_status;
	private Server server;
	private LoginInfo loginInfo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_complete_fragment, container, false);
		text_status = (TextView) v.findViewById(R.id.textView);
		prg_status = (ProgressBar) v.findViewById(R.id.progress);
		btn_next = (Button) v.findViewById(R.id.next);
		btn_next.setOnClickListener(this);

		btn_cancel = (Button) v.findViewById(R.id.cancel);
		btn_cancel.setOnClickListener(this);

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

		server = assistant.getServer();
		server.addListener(this);

		if (server.hasUserAuth()) {//Already logged in
			started = true;
			showSuccess();
		} else {//Still need to login
			showProgress();
			if (!started) {// not already underway
				String gcmID = GcmHelper.getGcmId();
				String deviceID = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
				loginInfo = new LoginInfo(assistant.getName(), deviceID, gcmID, PushConfig.MODE_GCM);
				server.login(loginInfo);
				started = true;
			}
		}
	}

	private void showSuccess() {
		btn_next.setEnabled(true);
		prg_status.setVisibility(View.GONE);
		text_status.setText(R.string.connected);
	}

	private void showProgress() {
		prg_status.setVisibility(View.VISIBLE);
		text_status.setText(R.string.connecting);
	}

	@Override
	public void onStop() {
		super.onStop();

		server.removeListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v == btn_next)
			assistant.finish(true);
		else
			assistant.finish(false);
	}
	@Override
	public void onLoginSuccessful() {
		showSuccess();
	}

	@Override
	public void onConnectionFailed(Exception e, int status) {
		showFailure(e, status);
	}

	@Override
	public void onServerInfoChanged(ServerInfo serverInfo) {

	}

	@Override
	public void onAvailableGroupsChanged(List<Group> groups) {

	}

	@Override
	public Handler getCallbackHandler() {
		return Threading.getUiExecutor();
	}

	private void showFailure(Exception e, int status) {
		prg_status.setVisibility(View.GONE);
		if (Util.isHttpPasswordIncorrect(status)) {
			text_status.setText(R.string.password_incorrect_try_again);
			DialogFragment dialog = new AssistantPasswordDialogFragment();
			dialog.show(getChildFragmentManager(), AssistantPasswordDialogFragment.TAG);
		} else {
			text_status.setText(R.string.connection_failure);
		}
	}

	@Override
	public void retryWithPassword(String password) {
		Log.i(THIS, "Retrying with new password...");
		server.setGroupPassword(password);
		server.login(loginInfo);
	}
}
