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
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.TransitionDrawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.zxing.integration.android.IntentIntegrator;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IServer;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.pictures.IPictureManager;
import de.stadtrallye.rallyesoft.model.pictures.PictureManager;
import de.stadtrallye.rallyesoft.net.NfcCallback;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.uimodel.IPictureHandler;
import de.stadtrallye.rallyesoft.uimodel.IProgressUI;
import de.stadtrallye.rallyesoft.uimodel.ITabActivity;
import de.stadtrallye.rallyesoft.uimodel.RallyeTabManager;
import de.stadtrallye.rallyesoft.uimodel.TabManager;
import de.stadtrallye.rallyesoft.util.converters.Serialization;
import de.wirsch.gcm.GcmHelper;

public class MainActivity extends ActionBarActivity implements IProgressUI, ITabActivity, IPictureHandler, IServer.ICurrentServerListener, SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String THIS = MainActivity.class.getSimpleName();

	private android.support.v7.app.ActionBar actionBar;

	private boolean progressCircle = false;

	private Integer lastTab;

	private RallyeTabManager tabManager;
	private IPictureManager.IPicture picture = null;
	private TransitionDrawable logo;
	private Server server;
	private boolean hasServerChanged = false;
	private boolean pref_autoUpload;
	private SharedPreferences pref;
	private PictureManager pictureManager;
	private boolean running = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		android.support.v4.app.FragmentManager.enableDebugLogging(true);

		// Layout, Title, ProgressCircle etc.

		setTitle(R.string.app_name);
		setContentView(R.layout.main);

		DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.material_blue_700));

		setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

//		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
//		logo = (TransitionDrawable) getResources().getDrawable(R.drawable.transition_logo);
//		actionBar.setHomeAsUpIndicator(logo);

		// Check if Google Play Services is working
		int errorCode;
		if ((errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)) != ConnectionResult.SUCCESS) {
			GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
			finish();
		}

		// Google Cloud Messaging Init
		//PushInit.ensureRegistration(this);
		GcmHelper.ensureRegistration(getApplicationContext());

		Storage.aquireStorage(getApplicationContext(), this);
		pref = Storage.getAppPreferences();
		pref.registerOnSharedPreferenceChangeListener(this);
		pref_autoUpload = pref.getBoolean("auto_upload", true);
		pictureManager = Storage.getPictureManager();


		// Initialize Model
		Server.addListener(this);
		server = Server.getCurrentServer();

		// Manages all fragments that will be displayed as Tabs in this activity
		tabManager = new RallyeTabManager(this, server, drawerLayout);

//		tabManager.setArguments(RallyeTabManager.TAB_MAP, getDefaultMapOptions(null));// this map is currently not used anyway, so for the time being we always do lateInitialization
		// Recover Last State
		tabManager.restoreState(savedInstanceState);

//		forceOverflowMenu(); // Force devices with hardware menu key to display the modern overflow menu

		// Initialize NFC ability
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			initNFC();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.i(THIS, "Receiving Intent");

		if (intent != null && intent.hasExtra(Std.TAB)) {
			Log.i(THIS, "Receiving Intent with Tab");
			if (intent.getStringExtra(Std.TAB).equals(Std.CHATROOM)) {
				int chatroom = intent.getIntExtra(Std.CHATROOM, -1);
				int chatID = intent.getIntExtra(Std.CHAT_ID, -1);
				Bundle b = new Bundle();
				b.putInt(Std.CHATROOM, chatroom);
				b.putInt(Std.CHAT_ID, chatID);
				tabManager.setArguments(RallyeTabManager.TAB_CHAT, b);
				tabManager.switchToTab(RallyeTabManager.TAB_CHAT);
			}
		}
	}

	@TargetApi(14)
	private void initNFC() {
		NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
		if (nfc != null) {
			nfc.setNdefPushMessageCallback(new NfcCallback(server), this);
		}
	}

	/**
	 * Force the Menu SoftButton even if hardware button present (only for 4.0 and greater)
	 */
//	private void forceOverflowMenu() {
//		try {
//			ViewConfiguration config = ViewConfiguration.get(this);
//			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
//			if (menuKeyField != null) {
//				menuKeyField.setAccessible(true);
//				menuKeyField.setBoolean(config, false);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setSupportProgressBarIndeterminateVisibility(false);
		tabManager.onPostCreate();
	}

	@Override
	public void onStart() {
		super.onStart();

		if (hasServerChanged) {
			updateServerState();
		}

//		if (isLoggedIn())
//			logo.startTransition(0);
//		else
//			logo.resetTransition();

		tabManager.showTab();

		running = true;
	}

	private void updateServerState() {
		tabManager.setServer(server);

//		onConnectionStateChange(model.getConnectionState());
		hasServerChanged = false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		tabManager.saveState(outState);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = tabManager.isMenuOpen();
		boolean loggedIn = isLoggedIn();

		menu.findItem(R.id.menu_logout).setVisible(!drawerOpen && loggedIn);
		menu.findItem(R.id.menu_share_barcode).setVisible(!drawerOpen).setEnabled(loggedIn);
//		menu.findItem(R.id.menu_reconnect).setVisible(!drawerOpen && model.canReconnect());

		return true;
	}

	private boolean isLoggedIn() {
		return server != null && server.hasUserAuth();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				return tabManager.onAndroidHome(item);
			case R.id.menu_login:
				Intent intent = new Intent(this, ConnectionAssistantActivity.class);
				lastTab = tabManager.getCurrentTab();
//				tabManager.switchToTab(RallyeTabManager.TAB_WAIT_FOR_MODEL);
				startActivityForResult(intent, ConnectionAssistantActivity.REQUEST_CODE);
				break;
			case R.id.menu_logout:
				new AlertDialog.Builder(this).setTitle(R.string.logout).setMessage(R.string.are_you_sure).setCancelable(true).setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						server.logout();
					}
				}).create().show();
				server.tryLogout();
				break;
			case R.id.menu_upload_overview:
				intent = new Intent(this, UploadOverviewActivity.class);
				startActivity(intent);
				break;
			case R.id.menu_share_barcode:
				IntentIntegrator zx = new IntentIntegrator(this);
				ObjectMapper mapper = Serialization.getJsonInstance();
				try {
					zx.shareText(mapper.writeValueAsString(server.exportLogin()));
				} catch (JsonProcessingException e) {
					Log.e(THIS, "Could not serialize exported Login", e);
					Toast.makeText(this, R.string.export_barcode_error, Toast.LENGTH_SHORT).show();
				}
				break;
//			case R.id.menu_reconnect: //there is no more temporary offline, we cannot know if push messages will reach us
//				model.reconnect();
//				break;
			case R.id.menu_settings:
				intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		if (!tabManager.onBackPressed())
			super.onBackPressed();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		tabManager.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause() {
//		model.saveState();// model should always save important data immediately

		super.onPause();
	}

	@Override
	protected void onStop() {
		running = false;

		super.onStop();
	}

	/**git status
	 * 
	 * Passed-Through to Model (if not kept for ConfigurationChange) and GCMRegistrar
	 */
	@Override
	protected void onDestroy() {
		Log.i(THIS, "Destroying...");

		server = null;
		Server.removeListener(this);

		Storage.releaseStorage(this);
		pref.unregisterOnSharedPreferenceChangeListener(this);

		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Android changes the upper 16 bits of a request generated from a fragment, so that
		// it can deliver the result back to the fragment.
		// We want to handle the result here, so we only look at the lower bits
		if ((requestCode&0xffff) == ConnectionAssistantActivity.REQUEST_CODE) {
			Log.v(THIS, "ConnectionAssistant finished with "+ resultCode);
			if (resultCode == Activity.RESULT_OK) {
				Log.i(THIS, "ConnectionAssistant has connected to a new Server");

//				model.removeListener(this);
//				model = Model.getInstance(getApplicationContext());
//				model.addListener(this);
//				updateServerState();
			}
			findViewById(R.id.content_frame).post(new Runnable() {
				@Override
				public void run() {
					tabManager.switchToTab(RallyeTabManager.TAB_OVERVIEW);
				}
			});
		} else if ((requestCode&0xffff) == SubmitNewSolutionActivity.REQUEST_CODE) {
			Log.i(THIS, "Task Submission");
			if (resultCode == Activity.RESULT_OK) {
//				Log.i(THIS, "Submitted: "+ data.getExtras());
			}
		} else {
			Log.i(THIS, "Received ActivityResult: Req: "+ requestCode +", res: "+ resultCode + ", intent: "+ data);
			picture = pictureManager.onActivityResult(requestCode, resultCode, data);



			/*if (tabManager.getCurrentTab() == RallyeTabManager.TAB_CHAT && picture != null) {
				IPictureTakenListener chatTab = (IPictureTakenListener) tabManager.getActiveFragment();
				chatTab.pictureTaken(picture);
			}*/

//			ImageLoader.getInstance().handleSlowNetwork();
		}

		super.onActivityResult(requestCode, resultCode, data);//TODO: this call is redundant, it only distributes teh result to the originating fragment, (we handle this ourselves)
	}

	/**
	 * IModelListener
	 */
//	@Override
//	public void onConnectionStateChange(ConnectionState newState) {
//		switch (newState) {//TODO: Add "No Network" status to UI (requires Model to have a "No Network" status) [Model has NoNetwork status, but never uses it] [Listen to Network Status Changes]
//			case Disconnecting:
//			case Connecting:
//				activateProgressAnimation();
//				break;
//			case Disconnected:
//			case Connected:
//			default:
//				deactivateProgressAnimation();
//		}
//
//		tabManager.conditionChange();
//
//		switch (newState) {
//			case Connected:
////				d.setColorFilter(null);
////				actionBar.setIcon(d);
//				logo.startTransition(500);
//				break;
//			default:
////				d.setColorFilter(0x66666666, Mode.MULTIPLY);
////				actionBar.setIcon(d);
//				logo.reverseTransition(500);
//		}
//	}

//	@Override
//	public void onConnectionFailed(Exception e) {
//		Toast.makeText(this, getString(R.string.connection_failure) +": "+ e.toString(), Toast.LENGTH_LONG).show();
//	}

//	@Override
//	public void onMapConfigChange() {
//		tabManager.setArguments(RallyeTabManager.TAB_MAP, getDefaultMapOptions(server));
//	}

	/**
	 * ITabActivity
	 */
	@Override
	public TabManager getTabManager() {
		return tabManager;
	}

	/**
	 * IProgressUI
	 */
	@Override
	public void activateProgressAnimation() {
		progressCircle = true;
		setSupportProgressBarIndeterminateVisibility(true);
	}

	/**
	 * IProgressUI
	 */
	@Override
	public void deactivateProgressAnimation() {
		if (progressCircle) {
			progressCircle = false;
			setSupportProgressBarIndeterminateVisibility(false);
		}
	}


	@Override//TODO remove / revise
	public IPictureManager.IPicture getPicture() {
		return picture;
	}

	@Override
	public boolean hasPicture() {
		return picture != null;
	}

	@Override
	public void discardPicture() {
		picture = null;
	}

	@Override
	public void onNewCurrentServer(Server server) {
		this.server = server;

//		if (isLoggedIn()) {
//			logo.startTransition(500);
//		} else {
//			logo.reverseTransition(500);
//		}

		if (running) {
			tabManager.setServer(server);
		} else {
			this.hasServerChanged = true;
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("auto_upload")) {
			pref_autoUpload = sharedPreferences.getBoolean("auto_upload", true);//TODO prettier
		}
	}
}
