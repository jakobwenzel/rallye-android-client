package de.stadtrallye.rallyesoft;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.zxing.integration.android.IntentIntegrator;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.IModel.ConnectionState;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.net.NfcCallback;
import de.stadtrallye.rallyesoft.net.PushInit;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.IPictureTakenListener;
import de.stadtrallye.rallyesoft.uimodel.IProgressUI;
import de.stadtrallye.rallyesoft.uimodel.ITabActivity;
import de.stadtrallye.rallyesoft.uimodel.RallyeTabManager;
import de.stadtrallye.rallyesoft.uimodel.TabManager;
import de.stadtrallye.rallyesoft.util.ImageLocation;

import static de.stadtrallye.rallyesoft.uimodel.Util.getDefaultMapOptions;

public class MainActivity extends SherlockFragmentActivity implements IModelActivity, IModel.IModelListener, IProgressUI, ITabActivity {

	private static final String THIS = MainActivity.class.getSimpleName();

	private IModel model;
	private boolean keepModel = false;
	private boolean progressCircle = false;

	private Integer lastTab;

	private RallyeTabManager tabManager;
//	private DrawerLayout drawerLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Layout, Title, ProgressCircle etc.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setTitle(R.string.app_name);
		setContentView(R.layout.main);

		DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(true);

		// Google Cloud Messaging Init
		PushInit.ensureRegistration(this);

		// Check if Google Play Services is working
		int errorCode;
		if ((errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)) != ConnectionResult.SUCCESS)
			GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();

		// Initialize Model
		model = (IModel) getLastCustomNonConfigurationInstance();
		if (model == null)
			model = Model.getInstance(getApplicationContext());
		model.addListener(this);
		keepModel = false;

		// Manages all fragments that will be displayed as Tabs in this activity
		tabManager = new RallyeTabManager(this, model, drawerLayout);
		tabManager.setArguments(RallyeTabManager.TAB_MAP, getDefaultMapOptions(model));
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
			nfc.setNdefPushMessageCallback(new NfcCallback(model), this);
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

		tabManager.onPostCreate();
		initializeState();
	}

	@Override
	public void onStart() {
		super.onStart();

		tabManager.showTab();
	}

	private void initializeState() {
		setSupportProgressBarIndeterminateVisibility(false);
		tabManager.setModel(model);
		onConnectionStateChange(model.getConnectionState());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		tabManager.saveState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = tabManager.isMenuOpen();
		boolean connected = model.isConnected();

		menu.findItem(R.id.menu_logout).setVisible(!drawerOpen && connected);
		menu.findItem(R.id.menu_share_barcode).setVisible(!drawerOpen).setEnabled(connected);
		menu.findItem(R.id.menu_reconnect).setVisible(!drawerOpen && model.canReconnect());

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				return tabManager.onAndroidHome();
			case R.id.menu_login:
				Intent intent = new Intent(this, ConnectionAssistant.class);
				lastTab = tabManager.getCurrentTab();
				tabManager.switchToTab(RallyeTabManager.TAB_WAIT_FOR_MODEL);
				startActivityForResult(intent, ConnectionAssistant.REQUEST_CODE);
				break;
			case R.id.menu_logout:
				model.logout();
				break;
			case R.id.menu_share_barcode:
				IntentIntegrator zx = new IntentIntegrator(this);
				zx.shareText(model.getLogin().toJSON());
				break;
			case R.id.menu_reconnect:
				model.reconnect();
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

	/**
	 * Called if e.g. the App is rotated
	 * => save the model
	 */
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		keepModel = true;
		return model;
	}

	@Override
	protected void onPause() {
		model.saveState();

		super.onPause();
	}

	/**git status
	 * 
	 * Passed-Through to Model (if not kept for ConfigurationChange) and GCMRegistrar
	 */
	@Override
	protected void onDestroy() {
		Log.i(THIS, "Destroying...");

		model.removeListener(this);

		if (!keepModel) // Only destroy the model if we do not need it again after the configuration change
			model.destroy();

		try {
			GCMRegistrar.onDestroy(this);
		} catch (Exception e) {
			Log.e(THIS, "GCMRegistrar failed to destroy", e);
		}

		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ConnectionAssistant.REQUEST_CODE) {
			Log.i(THIS, "ConnectionAssistant finished with "+ resultCode);
			if (resultCode == Activity.RESULT_OK) {
				Log.i(THIS, "New Connection, refresh Model, refresh everything depending on it");
				model.removeListener(this);
				model = Model.getInstance(getApplicationContext());
				model.addListener(this);
				initializeState();
			}
			findViewById(R.id.content_frame).postDelayed(new Runnable() {
				@Override
				public void run() {
					if (lastTab == null)
						lastTab = RallyeTabManager.TAB_OVERVIEW;
					tabManager.switchToTab(lastTab);
					Log.w(THIS, "switching back to tab: "+ lastTab);
					lastTab = null;
					onConnectionStateChange(model.getConnectionState());
				}
			}, 1000);
		} else if (requestCode == SubmitNewSolution.REQUEST_CODE) {
			Log.i(THIS, "Task Submission");
			if (resultCode == Activity.RESULT_OK) {

			}
		} else {
			final IPictureTakenListener.Picture picture = ImageLocation.imageResult(requestCode, resultCode, data, getApplicationContext(), true);

			if (tabManager.getCurrentTab() == RallyeTabManager.TAB_CHAT && picture != null) {
				IPictureTakenListener chatTab = (IPictureTakenListener) tabManager.getActiveFragment();
				chatTab.pictureTaken(picture);
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * IModelListener
	 */
	@Override
	public void onConnectionStateChange(ConnectionState newState) {
		ActionBar ab = getSupportActionBar();
		switch (newState) {//TODO: Add "No Network" status to UI (requires Model to have a "No Network" status) [Model has NoNetwork status, but never uses it] [Listen to Network Status Changes]
			case Disconnecting:
			case Connecting:
				activateProgressAnimation();
				break;
			case Disconnected:
			case Connected:
			default:
				deactivateProgressAnimation();
		}

		tabManager.conditionChange();

		Drawable d = getResources().getDrawable(R.drawable.ic_launcher);

		switch (newState) {
			case Connected:
				d.setColorFilter(null);
				ab.setIcon(d);
				break;
			default:
				d.setColorFilter(0x66666666, Mode.MULTIPLY);
				ab.setIcon(d);
		}
	}

	@Override
	public void onConnectionFailed(Exception e, ConnectionState fallbackState) {
		Toast.makeText(this, getString(R.string.connection_failure) +": "+ e.toString(), Toast.LENGTH_LONG).show();

		onConnectionStateChange(fallbackState);
	}

//	@Override
	public void onMapConfigChange() {
		tabManager.setArguments(RallyeTabManager.TAB_MAP, getDefaultMapOptions(model));
	}

	@Override
	public void onServerInfoChange(ServerInfo info) {

	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {

	}

	/**
	 * IModelActivity
	 */
	@Override
	public IModel getModel() {
		return model;
	}

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


}
