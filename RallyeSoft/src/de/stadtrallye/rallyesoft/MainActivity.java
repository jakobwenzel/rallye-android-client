package de.stadtrallye.rallyesoft;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.gcm.GCMRegistrar;
import com.google.zxing.integration.android.IntentIntegrator;

import java.lang.reflect.Field;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.IModel.ConnectionStatus;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.net.NfcCallback;
import de.stadtrallye.rallyesoft.net.PushInit;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.IProgressUI;
import de.stadtrallye.rallyesoft.uimodel.MenuItemImpl;
import de.stadtrallye.rallyesoft.uimodel.RallyeTabManager;

import static de.stadtrallye.rallyesoft.uimodel.Util.getDefaultMapOptions;

public class MainActivity extends SherlockFragmentActivity implements AdapterView.OnItemClickListener, IModelActivity,
																		IModel.IModelListener, IProgressUI {

	private static final String THIS = MainActivity.class.getSimpleName();

	private IModel model;
	private boolean keepModel = false;
	private boolean progressCircle = false;
	private String[] nav;

//	private SlidingMenu sm;
	private RallyeTabManager tabManager;
	private DrawerLayout drawerLayout;
	private ListView dashboard;
	private ActionBarDrawerToggle drawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initFrame();

		initSlidingMenu();

		// Ray's INIT
		PushInit.ensureRegistration(this);
		model = (IModel) getLastCustomNonConfigurationInstance();
		if (model == null)
			model = Model.getModel(getApplicationContext());
		model.addListener(this);
		keepModel = false;

		tabManager = new RallyeTabManager(this, model);
		tabManager.setArguments(RallyeTabManager.TAB_MAP, getDefaultMapOptions(model));

		// Recover Last State
		tabManager.restoreState(savedInstanceState);

		forceOverflowMenu();

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			initNFC();
		}

	}

	private ActionBar initFrame() {
		// Title and Content
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setTitle(R.string.title_main);
		setContentView(R.layout.main);

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(true);

		return ab;
	}

	private void initSlidingMenu() {
//		setBehindContentView(R.layout.dashboard_main);
//		setSlidingActionBarEnabled(false);

		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
				R.drawable.ic_drawer, R.string.abs__action_bar_home_description, R.string.abs__action_bar_up_description) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(tabManager.getCurrentTitle());
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(R.string.dash_menu);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};

		// Set the drawer toggle as the DrawerListener
		drawerLayout.setDrawerListener(drawerToggle);

		// Settings for SideBar
//		sm = getSlidingMenu();
//		sm.setShadowWidthRes(R.dimen.shadow_width);
//		sm.setShadowDrawable(R.drawable.defaultshadow);
//		sm.setSelectorEnabled(true);
//		sm.setSelectorDrawable(R.drawable.arrow);
//		sm.setBehindWidthRes(R.dimen.slidingmenu_width);
//		sm.setBehindScrollScale(0);
//		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);

		// Populate SideBar
		nav = getResources().getStringArray(R.array.dashboard_entries);

		dashboard = (ListView) findViewById(R.id.left_drawer);
		//TODO: own Adapter to disable elements if offline/highlight current element and set the SlidingMenu selector as soon as the first View has been instantiated
		ArrayAdapter<String> dashAdapter = new ArrayAdapter<>(this, R.layout.dashboard_item, android.R.id.text1, nav);
		dashboard.setAdapter(dashAdapter);
		dashboard.setOnItemClickListener(this);
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
	private void forceOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		drawerToggle.syncState();
	}

	@Override
	public void onStart() {
		super.onStart();

		setSupportProgressBarIndeterminateVisibility(false);
		onConnectionStatusChange(model.getConnectionStatus());

		tabManager.showTab();
	}

	/**
	 * AdapterView.OnItemClickListener
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		tabManager.switchToTab(pos);
		drawerLayout.closeDrawer(dashboard);
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
		boolean drawerOpen = drawerLayout.isDrawerOpen(dashboard);

		boolean act = model.isConnected();
		MenuItem logout = menu.findItem(R.id.menu_logout);
		logout.setVisible(!drawerOpen).setEnabled(act);
		MenuItem share = menu.findItem(R.id.menu_share_barcode);
		share.setVisible(!drawerOpen).setEnabled(act);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(new MenuItemImpl(item)))
			return true;

		switch (item.getItemId()) {
//			case android.R.id.home:
//				toggle();
//				break;
			case R.id.menu_login:
				Intent intent = new Intent(this, ConnectionAssistant.class);
				startActivityForResult(intent, ConnectionAssistant.REQUEST_CODE);
				break;
			case R.id.menu_logout:
				model.logout();
				break;
			case R.id.menu_share_barcode:
				IntentIntegrator zx = new IntentIntegrator(this);
				zx.shareText(model.getLogin().toJSON());
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		drawerToggle.onConfigurationChanged(newConfig);
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

	/**
	 * Passed-Through to Model (if not kept for ConfigurationChange) and GCMRegistrar
	 */
	@Override
	protected void onDestroy() {
		Log.i(THIS, "Destroying...");

		model.removeListener(this);

		if (!keepModel) // Only destroy the model if we do not need it again after the configuration change
			model.onDestroy();

		GCMRegistrar.onDestroy(this);

		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ConnectionAssistant.REQUEST_CODE) {
			if (resultCode > 0) {
				model.removeListener(this);
				model = Model.getModel(getApplicationContext());
				model.addListener(this);
				//TODO: force refresh
			}
		} else if (data != null) {
			Uri uri = data.getData();

			if (uri != null) {
				try {
					//User has picked an image.
					Cursor cursor = getContentResolver().query(uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
					cursor.moveToFirst();

					//Link to the image
					final String imageFilePath = cursor.getString(0);

					Log.i(THIS, "Picture taken/selected: " + imageFilePath);

					cursor.close();

					Intent intent = new Intent(this, UploadService.class);
					intent.putExtra(Std.PIC, imageFilePath);
					intent.putExtra(Std.MIME, "image/jpeg");
					String hash = String.valueOf(imageFilePath.hashCode());//TODO: use hash
					intent.putExtra(Std.HASH, hash);
					startService(intent);
				} catch (Exception e) {
					Log.e(THIS, "Failed to select Picture", e);
				}
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * IModelListener
	 */
	@Override
	public void onConnectionStatusChange(ConnectionStatus status) {
		ActionBar ab = getSupportActionBar();
		switch (status) {//TODO: Add "No Network" status to UI (requires Model to have a "No Network" status) [Model has NoNetwork status, but never uses it] [Listen to Network Status Changes]
			case Disconnecting:

			case Connecting:
				activateProgressAnimation();
				break;
			case Disconnected:
				tabManager.conditionChange();
			case Connected:
			default:
				deactivateProgressAnimation();
		}

		Drawable d = getResources().getDrawable(R.drawable.ic_launcher);

		switch (status) {
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
	public void onConnectionFailed(Exception e, ConnectionStatus lastStatus) {
		Toast.makeText(this, getString(R.string.connection_failure), Toast.LENGTH_SHORT).show();

		onConnectionStatusChange(lastStatus);
	}

	@Override
	public void onServerConfigChange() {
		tabManager.setArguments(RallyeTabManager.TAB_MAP, getDefaultMapOptions(model));
	}

	/**
	 * IModelActivity
	 */
	@Override
	public IModel getModel() {
		return model;
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
