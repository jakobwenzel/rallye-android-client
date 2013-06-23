package de.stadtrallye.rallyesoft;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
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
 * Created by Ramon on 19.06.13.
 */
public class ConnectionAssistant extends SherlockFragmentActivity implements IConnectionAssistant {

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

		// Titel und Inhalt
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setTitle(R.string.connection_assistant);
		setContentView(R.layout.connection_assistant);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(true);

		if (savedInstanceState != null) {
			step = savedInstanceState.getInt(Std.STEP);
			server = savedInstanceState.getString(Std.SERVER);
			pass = savedInstanceState.getString(Std.PASSWORD);
			groupID = savedInstanceState.getInt(Std.GROUP_ID);
			name = savedInstanceState.getString(Std.NAME);
		}

		model = Model.createEmptyModel(getApplicationContext());

		//Create FragmentHandlers
		steps = new ArrayList<>();
		steps.add(new FragmentHandler<>("server", AssistantServerFragment.class));
		steps.add(new FragmentHandler<>("groups", AssistantGroupsFragment.class));
		steps.add(new FragmentHandler<>("auth", AssistantAuthFragment.class));
		steps.add(new FragmentHandler<>("complete", AssistantCompleteFragment.class));
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

		private String tag;
		private Class<T> clz;
		private Bundle arg;

		public FragmentHandler(String tag, Class<T> clz) {
			this.tag = tag;
			this.clz = clz;
		}

		public void setArguments(Bundle arg) {
			this.arg = arg;
		}

		public Fragment getFragment() {
			Fragment f = getSupportFragmentManager().findFragmentByTag(tag);

			if (f == null) {
				if (arg == null)
					f = Fragment.instantiate(ConnectionAssistant.this, clz.getName());
				else
					f = Fragment.instantiate(ConnectionAssistant.this, clz.getName(), arg);
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
		scan_qr.setIcon(R.drawable.scan_qr);
		scan_qr.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		FragmentHandler<?> f = steps.get(step-1);
		ft.replace(R.id.fragments, f.getFragment(), f.getTag()).commit();

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
		ServerLogin l = null;
		try {
			l = ServerLogin.fromJSON(login);

			server = l.getServer().toString();
			groupID = l.getGroupID();
			pass = l.getGroupPassword();

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

			ft.replace(R.id.fragments, f.getFragment(), f.getTag()).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
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
			Model.switchToNew(model);
			model = null;
		} else {
			model.onDestroy();
			model = null;
		}

		setResult(1);

		super.finish();
	}

	@Override
	protected void onDestroy() {
		if (model != null)
			model.onDestroy();

		super.onDestroy();
	}

	/**
	 * IProgressUI
	 */
	@Override
	public void activateProgressAnimation() {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	/**
	 * IProgressUI
	 */
	@Override
	public void deactivateProgressAnimation() {
		setSupportProgressBarIndeterminateVisibility(false);
	}

}
