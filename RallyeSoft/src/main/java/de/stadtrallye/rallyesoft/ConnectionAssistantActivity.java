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
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

import de.rallye.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.fragments.AssistantAuthFragment;
import de.stadtrallye.rallyesoft.fragments.AssistantCompleteFragment;
import de.stadtrallye.rallyesoft.fragments.AssistantGroupsFragment;
import de.stadtrallye.rallyesoft.fragments.AssistantServerFragment;
import de.stadtrallye.rallyesoft.net.Server;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;
import de.stadtrallye.rallyesoft.util.converters.Serialization;

/**
 * Activity that hosts an IConnectionAssistant and several Fragments containing the guided login
 * User inputs and server configurations are saved here, so they can be accessed from all pages of the assistant
 */
public class ConnectionAssistantActivity extends FragmentActivity implements IConnectionAssistant, LoaderManager.LoaderCallbacks<Cursor> {

	public static final int REQUEST_CODE = 1336;
	private static final String THIS = ConnectionAssistantActivity.class.getSimpleName();

	private ArrayList<FragmentHandler<?>> steps;
	private int step = 1;
	private boolean fastForward = false;

	private Server server;
	private String name;
	private String suggestedName;

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

		Storage.aquireStorage(getApplicationContext(), this);

		if (savedInstanceState != null) {
			step = savedInstanceState.getInt(Std.STEP);
			server = Server.load(savedInstanceState.getString(Std.SERVER));
			name = savedInstanceState.getString(Std.NAME);
		}

		//Create FragmentHandlers
		steps = new ArrayList<FragmentHandler<?>>();
		steps.add(new FragmentHandler<AssistantServerFragment>("server", AssistantServerFragment.class));
		steps.add(new FragmentHandler<AssistantGroupsFragment>("groups", AssistantGroupsFragment.class));
		steps.add(new FragmentHandler<AssistantAuthFragment>("auth", AssistantAuthFragment.class));
		steps.add(new FragmentHandler<AssistantCompleteFragment>("complete", AssistantCompleteFragment.class));

		getLoaderManager().initLoader(0, null, this);
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

	private static final String[] PROFILE_PROJECTION = {ContactsContract.Profile.DISPLAY_NAME_PRIMARY};

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getApplicationContext(), ContactsContract.Profile.CONTENT_URI, PROFILE_PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		suggestedName = data.getString(0);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

	@Override
	public String getSuggestedName() {
		return suggestedName;
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
		outState.putString(Std.SERVER, server.serialize());
		outState.putString(Std.NAME, name);
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

	/**
	 * Extract login info from JSON
	 * @param serverLoginJSON
	 */
	private void readServerLoginJSON(String serverLoginJSON) {
		ServerLogin l;
		try {
			ObjectMapper mapper = Serialization.getInstance();
			l = mapper.readValue(serverLoginJSON, ServerLogin.class);

			setServer(new Server(l.getAddress()));
			setGroup(l.getGroupID());
			server.setGroupPassword(l.getGroupPassword());

			fastForward = true;
		} catch (Exception e) {
			Log.e(THIS, "Could not deserialize ServerLogin from JSON", e);
			Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
		}
	}

	private void processNfcIntent(Intent intent) {
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		// only one message sent during the beam
		NdefMessage msg = (NdefMessage) rawMsgs[0];

		readServerLoginJSON(new String(msg.getRecords()[0].getPayload()));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IntentIntegrator.REQUEST_CODE) {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
			if (scanResult == null) return;

			readServerLoginJSON(scanResult.getContents());
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
		server.setGroupPassword(pass);
	}

	@Override
	public int getGroup() {
		return server.getGroupID();
	}

	@Override
	public String getPass() {
		return server.getGroupPassword();
	}

	@Override
	public String getName() {
		return name;
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


	@Override
	public void setServer(Server server) {
		this.server = server;
	}

	@Override
	public Server getServer() {
		return server;
	}
	
	@Override
	public void setGroup(int groupID) {
		server.setGroupID(groupID);
	}

	@Override
	public void finish(boolean acceptNewConnection) {
		if (acceptNewConnection && server.hasUserAuth()) {
			Server.setCurrentServer(server);
		} else {
			//server.destroy();
		}

		setResult((acceptNewConnection)? Activity.RESULT_OK : Activity.RESULT_CANCELED);

		super.finish();
	}

	@Override
	protected void onDestroy() {

		Storage.releaseStorage(this);

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
