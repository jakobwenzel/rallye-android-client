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

package de.stadtrallye.rallyesoft;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.net.MalformedURLException;
import java.util.ArrayList;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.fragments.AssistantAuthFragment;
import de.stadtrallye.rallyesoft.fragments.AssistantCompleteFragment;
import de.stadtrallye.rallyesoft.fragments.AssistantGroupsFragment;
import de.stadtrallye.rallyesoft.fragments.AssistantServerFragment;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Activity that hosts an IConnectionAssistant and several Fragments containing the guided login
 * User inputs and server configurations are saved here, so they can be accessed from all pages of the assistant
 */
public class ConnectionAssistantActivity extends FragmentActivity implements IConnectionAssistant {

	public static final int REQUEST_CODE = 1336;

	private IModel model;

	private ArrayList<FragmentHandler<?>> steps;
	private int step = 1;
	private boolean fastForward = false;

	private String server;
	private int groupID;
	private String name;
	private String pass;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Title and Content
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setTitle(R.string.connection_assistant);
//		setContentView(R.layout.connection_assistant);

		ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(true);

		if (savedInstanceState != null) {
			step = savedInstanceState.getInt(Std.STEP);
			server = savedInstanceState.getString(Std.SERVER);
			pass = savedInstanceState.getString(Std.PASSWORD);
			groupID = savedInstanceState.getInt(Std.GROUP_ID);
			name = savedInstanceState.getString(Std.NAME);
		}

		model = Model.createTemporaryModel(getApplicationContext());

		//Create FragmentHandlers
		steps = new ArrayList<FragmentHandler<?>>();
		steps.add(new FragmentHandler<AssistantServerFragment>("server", AssistantServerFragment.class));
		steps.add(new FragmentHandler<AssistantGroupsFragment>("groups", AssistantGroupsFragment.class));
		steps.add(new FragmentHandler<AssistantAuthFragment>("auth", AssistantAuthFragment.class));
		steps.add(new FragmentHandler<AssistantCompleteFragment>("complete", AssistantCompleteFragment.class));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			case R.id.scan_qr_login:
				IntentIntegrator zx = new IntentIntegrator(this);
				zx.initiateScan(IntentIntegrator.QR_CODE_TYPES);
				return true;
		}
		return false;
	}

	/**
	 * Envelops a Fragment, reuses a already existing Fragment otherwise instantiates a new one
	 * @author Ramon
	 *
	 * @param <T> Fragment Type to envelop
	 */
	private class FragmentHandler<T extends Fragment> {

		private final String tag;
		private final Class<T> clz;
		private Bundle arg;

		public FragmentHandler(String tag, Class<T> clz) {
			this.tag = tag;
			this.clz = clz;
		}

// --Commented out by Inspection START (22.09.13 02:46):
//		public void setArguments(Bundle arg) {
//			this.arg = arg;
//		}
// --Commented out by Inspection STOP (22.09.13 02:46)

		public Fragment getFragment() {
			Fragment f = getSupportFragmentManager().findFragmentByTag(tag);

			if (f == null) {
				if (arg == null)
					f = Fragment.instantiate(ConnectionAssistantActivity.this, clz.getName());
				else
					f = Fragment.instantiate(ConnectionAssistantActivity.this, clz.getName(), arg);
			}

			return f;
		}

		public String getTag() {
			return tag;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem scan_qr = menu.add(Menu.NONE, R.id.scan_qr_login, 10, R.string.scan_barcode);
		scan_qr.setIcon(R.drawable.ic_scan_qr_light);
		scan_qr.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		FragmentHandler<?> f = steps.get(step-1);
		ft.replace(android.R.id.content, f.getFragment(), f.getTag()).commit();

		if (fastForward) {
			while (step < steps.size()-1) {
				next();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(Std.STEP, step);
		outState.putString(Std.SERVER, server);
		outState.putString(Std.PASSWORD, pass);
		outState.putString(Std.NAME, name);
		outState.putInt(Std.GROUP_ID, groupID);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			processNfcIntent(getIntent());
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	private void setServerLoginJSON(String login) {
		ServerLogin l;
		try {
			l = ServerLogin.fromJSON(login);

			setServer(l.getServer().toString());
			setGroup(l.getGroupID());
			this.pass = l.getGroupPassword();

			fastForward = true;
		} catch (Exception e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}
	}

	private void processNfcIntent(Intent intent) {
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		// only one message sent during the beam
		NdefMessage msg = (NdefMessage) rawMsgs[0];

		setServerLoginJSON(new String(msg.getRecords()[0].getPayload()));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IntentIntegrator.REQUEST_CODE) {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
			if (scanResult == null) return;

			setServerLoginJSON(scanResult.getContents());
		}
	}

	@Override
	public void back() {
		getSupportFragmentManager().popBackStack();
		step--;
	}

	@Override
	public void setNameAndPass(String name, String pass) {
		this.name = name;
		this.pass = pass;
	}

	@Override
	public void login() {
		model.login(name, groupID, pass);
	}

	@Override
	public int getGroup() {
		return groupID;
	}

	@Override
	public String getPass() {
		return pass;
	}

	@Override
	public void next() {
		if (step < steps.size()) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			FragmentHandler<?> f = steps.get(step);

			ft.replace(android.R.id.content, f.getFragment(), f.getTag()).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
			step++;
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		step--;
	}


	/**
	 * IModelActivity
	 */
	@Override
	public IModel getModel() {
		return model;
	}

	@Override
	public void setServer(String server) throws MalformedURLException {
//		if (!model.isEmpty()) {
//			model.destroy();// Model can only have setServer called once!!
//			model = Model.createTemporaryModel(getApplicationContext());
//		}
		this.server = model.setServer(server);
	}

	@Override
	public String getServer() {
		return server;
	}
	
	@Override
	public void setGroup(int groupID) {
		this.groupID = groupID;
	}

	@Override
	public void finish(boolean acceptNewConnection) {
		if (acceptNewConnection && model.isConnected()) {
			model.acceptTemporaryModel();
			model = null;
		} else {
			model.destroy();
			model = null;
		}

		setResult((acceptNewConnection)? Activity.RESULT_OK : Activity.RESULT_CANCELED);

		super.finish();
	}

	@Override
	protected void onDestroy() {
		if (model != null)
			model.destroy();

		super.onDestroy();
	}

	/**
	 * IProgressUI
	 */
	@Override
	public void activateProgressAnimation() {
		setProgressBarIndeterminateVisibility(true);
	}

	/**
	 * IProgressUI
	 */
	@Override
	public void deactivateProgressAnimation() {
		setProgressBarIndeterminateVisibility(false);
	}

}
