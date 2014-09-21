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

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.net.MalformedURLException;
import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * 1. Page of ConnectionAssistant
 * Asks for Server details and tries the Connection (showing ServerInfo)
 */
public class AssistantServerFragment extends Fragment {

	private IConnectionAssistant assistant;

	private EditText server;
	private ImageView srv_image;
	private TextView srv_name;
	private TextView srv_desc;
	private Button next;
	private ImageLoader loader;
    private Spinner protocol;
	private EditText port;
	private Button test;
//	private EditText path;
	private ScrollView scrollView;
	private InfoManager infoManager;
	private ViewGroup server_info_manager;
	private ViewGroup server_info;
	private ViewGroup server_loading;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@TargetApi(11)
	private void setLayoutTransition(ViewGroup vg) {
		vg.setLayoutTransition(new LayoutTransition());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_server_fragment, container, false);

		scrollView = (ScrollView)v.findViewById(R.id.scrollView);

		protocol = (Spinner) v.findViewById(R.id.protocol);
		server = (EditText) v.findViewById(R.id.server);
		port = (EditText) v.findViewById(R.id.port);
//		path = (EditText) v.findViewById(R.id.path);

		port.setHint(Std.DEFAULT_PORT);
//		path.setHint(Std.DEFAULT_PATH);

//		path.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//			@Override
//			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//				if (actionId == EditorInfo.IME_ACTION_SEND) {
//					infoManager.startTest();
//				}
//				return false;
//			}
//		});

		test = (Button) v.findViewById(R.id.test);
		test.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				View focus = getActivity().getCurrentFocus();
				if (focus != null) {
					InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				}
				infoManager.startTest();
			}
		});

		server_info_manager = (ViewGroup) v.findViewById(R.id.info_manager);
		server_info = (ViewGroup) v.findViewById(R.id.server_info);
		server_loading = (ViewGroup) v.findViewById(R.id.loading);
		srv_image = (ImageView) v.findViewById(R.id.server_image);
		srv_name = (TextView) v.findViewById(R.id.server_name);
		srv_desc = (TextView) v.findViewById(R.id.server_desc);

		next = (Button) v.findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view) {
				assistant.next();
			}
		});

		loader = ImageLoader.getInstance();

		if (android.os.Build.VERSION.SDK_INT >= 11)
			setLayoutTransition((ViewGroup) v);

		if (infoManager == null)
			infoManager = new InfoManager(savedInstanceState);

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

		String s = assistant.getServer();
		if (s != null) {
			String[] parts = s.replaceAll("^(http|https)://([0-9A-Za-z_.-]+?):(\\d+?)$", "$1;$2;$3").split(";"); ///(\w+?)/?
			protocol.setSelection(parts[0].equals("http")? 0 : 1);
			port.setText(parts[2]);
			server.setText(parts[1]);
//			path.setText(parts[3]);
		}

		infoManager.restore();

		assistant.getModel().addListener(infoManager);
	}

	@Override
	public void onResume() {
		server.addTextChangedListener(infoManager);
		port.addTextChangedListener(infoManager);
//		path.addTextChangedListener(infoManager);

		super.onResume();
	}

	@Override
	public void onPause() {
		server.removeTextChangedListener(infoManager);
		port.removeTextChangedListener(infoManager);
//		path.removeTextChangedListener(infoManager);

		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();

		assistant.getModel().removeListener(infoManager);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		infoManager.onSaveInstanceState(outState);

		super.onSaveInstanceState(outState);
	}

	private String getServer() {
		String protocol = this.protocol.getSelectedItem().toString();
		String server = this.server.getText().toString();
		String port = this.port.getText().toString();
		if (port.equals(""))
			port = Std.DEFAULT_PORT;
//		String path = this.path.getText().toString();
//		if (path.equals(""))
//			path = Std.DEFAULT_PATH;

		return protocol +"://"+ server +":"+ port;
	}

//	private enum InfoState { clear, loading, complete }

	private class InfoManager implements IModel.IModelListener, ImageLoadingListener, TextWatcher {

//		private InfoState state;

		private boolean hasInfo, hasImage;

		public InfoManager(Bundle savedInstanceState) {
			if (savedInstanceState != null) {
				hasInfo = savedInstanceState.getBoolean(Std.SERVER+Std.CONNECTED, false);
				hasImage = savedInstanceState.getBoolean(Std.SERVER+Std.IMAGE, false);
				if (hasInfo && hasImage) {
					refresh();
//					state = InfoState.complete;
				} else {
					hide();
//					state = InfoState.clear;
				}
			} else {
				hasInfo = hasImage = false;
//				state = InfoState.clear;
				hide();
			}
		}

		public void onSaveInstanceState(Bundle out) {
			out.putBoolean(Std.SERVER+Std.CONNECTED, hasInfo);
			out.putBoolean(Std.SERVER+Std.IMAGE, hasImage);
		}

		public void reset() {
			hasInfo = hasImage = false;
//			state = InfoState.clear;
			hide();
		}

		public void restore() {
			if (hasImage && hasInfo) {
				displayServerInfo();
				IModel model = assistant.getModel();
				loader.displayImage(model.getServerPictureURL(), srv_image);
				onServerInfoChange(model.getServerInfo());
			} else
				hide();
		}

		public void startTest() {
//			state = InfoState.loading;
			try {
				displayLoadingState();
				String server = getServer();
				assistant.setServer(server);
				IModel model = assistant.getModel();
				loader.displayImage(model.getServerPictureURL(), srv_image, infoManager);
			} catch (MalformedURLException e) {
				Toast.makeText(getActivity(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
				hide();
			}
		}

		private void displayServerInfo() {
			server_info_manager.setVisibility(View.VISIBLE);
			server_loading.setVisibility(View.GONE);
			server_info.setVisibility(View.VISIBLE);
			test.setVisibility(View.GONE);
			next.setVisibility(View.VISIBLE);
			getView().post(new Runnable() {
				@Override
				public void run() {
					scrollView.scrollTo(0, next.getTop());
					next.requestFocus();
				}
			});
		}

		private void displayLoadingState() {
			server_info_manager.setVisibility(View.VISIBLE);
			server_loading.setVisibility(View.VISIBLE);
			server_info.setVisibility(View.GONE);
			test.setVisibility(View.GONE);
			next.setVisibility(View.GONE);
		}

		private void hide() {
			server_info_manager.setVisibility(View.GONE);
			test.setVisibility(View.VISIBLE);
			next.setVisibility(View.GONE);
		}

		private void refresh() {
			if (hasInfo && hasImage) {
				displayServerInfo();
//				state = InfoState.complete;
			}
		}

		private void failed() {
			loader.cancelDisplayTask(srv_image);
//			state = InfoState.clear;
			hide();
			Toast.makeText(getActivity(), R.string.connection_test_failed, Toast.LENGTH_SHORT).show();
			server.requestFocus();
		}

		@Override
		public void onConnectionStateChange(IModel.ConnectionState newState) {

		}

		@Override
		public void onConnectionFailed(Exception e, IModel.ConnectionState fallbackState) {
			hasInfo = false;
			failed();
		}

		@Override
		public void onServerInfoChange(ServerInfo info) {
			srv_name.setText(info.name);
			srv_desc.setText(info.description);

			hasInfo = true;
			refresh();
		}

		@Override
		public void onAvailableGroupsChange(List<Group> groups) {
		}

		@Override
		public void onLoadingStarted(String s, View view) {

		}

		@Override
		public void onLoadingFailed(String s, View view, FailReason failReason) {
			hasImage = false;
			failed();
		}

		@Override
		public void onLoadingComplete(String s, View view, Bitmap bitmap) {
			hasImage = true;
			refresh();
		}

		@Override
		public void onLoadingCancelled(String s, View view) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
//			state = InfoState.clear;
			hasImage = hasInfo = false;
			reset();
		}

		@Override
		public void afterTextChanged(Editable s) {
		}
	}
}