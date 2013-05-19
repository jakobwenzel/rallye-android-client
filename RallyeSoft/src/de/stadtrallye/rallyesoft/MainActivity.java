package de.stadtrallye.rallyesoft;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.fragments.ChatsFragment;
import de.stadtrallye.rallyesoft.fragments.GameMapFragment;
import de.stadtrallye.rallyesoft.fragments.LoginDialogFragment;
import de.stadtrallye.rallyesoft.fragments.OverviewFragment;
import de.stadtrallye.rallyesoft.fragments.TurnFragment;
import de.stadtrallye.rallyesoft.model.IConnectionStatusListener;
import de.stadtrallye.rallyesoft.model.IModel.ConnectionStatus;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.LatLngAdapter;
import de.stadtrallye.rallyesoft.model.structures.Login;
import de.stadtrallye.rallyesoft.net.NfcCallback;
import de.stadtrallye.rallyesoft.net.PushInit;
import de.stadtrallye.rallyesoft.uiadapter.IModelActivity;
import de.stadtrallye.rallyesoft.uiadapter.IProgressUI;

public class MainActivity extends SlidingFragmentActivity implements  ActionBar.OnNavigationListener, AdapterView.OnItemClickListener,
																		LoginDialogFragment.IDialogCallback, IModelActivity,
																		IConnectionStatusListener, IProgressUI {
	
	private static final String THIS = MainActivity.class.getSimpleName();
	
	public PushInit push;
	private Model model;
	private boolean keepModel = false;
	private boolean progressCircle = false;
//	private Fragment currentFragment;
	private int currentTab;
	private String[] nav;
	private ArrayList<FragmentHandler<?>> tabs;

	private FragmentHandler<GameMapFragment> mapFragmentHandler;
	
	private Login deferredLogin;

	private SlidingMenu sm;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		initFrame();
		
		initSlidingMenu();
		
		// Recover Last State
		if (savedInstanceState != null) {
			currentTab = savedInstanceState.getInt(Std.TAB, 0);
		} else {
			currentTab = 0;
		}
		
		// Ray's INIT
		PushInit.ensureRegistration(this);
		model = (Model) getLastCustomNonConfigurationInstance();
		if (model == null)
			model = Model.getInstance(getApplicationContext());
		model.addListener(this);
		keepModel = false;
		
		forceOverflowMenu();
		
		initFragments();
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			initNFC();
		}
		
	}
	
	private ActionBar initFrame() {
		// Titel und Inhalt
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		    
		setTitle(R.string.title_main);
		setContentView(R.layout.main);
		
		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(true);
        
        return ab;
	}
	
	private void initSlidingMenu() {
		setBehindContentView(R.layout.dashboard_main);
//		setSlidingActionBarEnabled(false);
		
		// Settings for SideBar
		sm = getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.defaultshadow);
		sm.setSelectorEnabled(true);
		sm.setSelectorDrawable(R.drawable.arrow);
		sm.setBehindWidthRes(R.dimen.slidingmenu_width);
		sm.setBehindScrollScale(0);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		
		// Populate SideBar
		nav = getResources().getStringArray(R.array.dashboard_entries);
		
        ListView dashboard = (ListView) sm.findViewById(R.id.dashboard_list);
      //TODO: own Adapter to disable elements if offline/highlight current element and set the SlidingMenu selector as soon as the first View has been instantiated
        ArrayAdapter<String> dashAdapter = new ArrayAdapter<String>(this, R.layout.dashboard_item, android.R.id.text1, nav);
        dashboard.setAdapter(dashAdapter);
        dashboard.setOnItemClickListener(this);
	}
	
	private void initFragments() {
		//Create FragmentHandlers
		tabs = new ArrayList<FragmentHandler<?>>();
		tabs.add(new FragmentHandler<OverviewFragment>("overview", OverviewFragment.class, false));
		tabs.add(mapFragmentHandler = new FragmentHandler<GameMapFragment>("map", GameMapFragment.class, false));
		tabs.add(new FragmentHandler<TurnFragment>("turn", TurnFragment.class, false));
		tabs.add(new FragmentHandler<ChatsFragment>("chat", ChatsFragment.class, true));
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
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
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
		final private boolean requiresOnline;

		public FragmentHandler(String tag, Class<T> clz, boolean requiresOnline) {
			this.tag = tag;
			this.clz = clz;
			this.requiresOnline = requiresOnline;
		}
		
		public void setArguments(Bundle arg) {
			this.arg = arg;
		}
		
		public Fragment getFragment() {
			Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
			
			if (f == null) {
				if (arg == null)
					f = Fragment.instantiate(MainActivity.this, clz.getName());
				else
					f = Fragment.instantiate(MainActivity.this, clz.getName(), arg);
			}
			
			return f;
		}
		
		public String getTag() {
			return tag;
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		setSupportProgressBarIndeterminateVisibility(false);
		onConnectionStatusChange(model.getConnectionStatus());
		
		onSwitchTab(currentTab, 0);
		sm.setSelectedView(getSelectedView(currentTab));
	}
	
	/**
	 * AdapterView.OnItemClickListener
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		if (currentTab != pos)
		{
//			getSupportActionBar().setSelectedNavigationItem(pos);
			onSwitchTab(pos, id);
		}
		getSlidingMenu().showContent();
	}
	
	private View getSelectedView(int pos) {
		View v = ((ListView)findViewById(R.id.dashboard_list)).getChildAt(pos);
		Log.d(THIS, "SlidingSelector on "+ v);
		return v;
	}
	
	private boolean isOnlineTab() {
		return tabs.get(currentTab).requiresOnline; 
	}
	
	private void switchTabFallback() {
		onSwitchTab(currentTab = 0, 0);
	}
	
	/**
	 * ActionBar.OnNavigationListener
	 */
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		return onSwitchTab(itemPosition, itemId);
	}
	
	private boolean onSwitchTab(int pos, long id) {
		
		setTitle(nav[pos]);
		
		switch (pos) {
		case 0://Overview
			break;
		case 1://Map
			Bundle b = new Bundle();//TODO: LatLngBounds for initial Camera [done internally with animation]
			GoogleMapOptions gmo = new GoogleMapOptions().compassEnabled(true);
			LatLng loc = LatLngAdapter.toGms(model.getMap().getMapLocation());
			if (loc != null) {
				gmo.camera(new CameraPosition(loc, model.getMap().getZoomLevel(), 0, 0));
			}
			b.putParcelable("MapOptions", gmo);
			mapFragmentHandler.setArguments(b);
			break;
		case 2://Next Play
			break;
		case 3://Chat
			break;
		default:
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.unsupported_link), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		FragmentHandler<?> tab = tabs.get(pos);
		
		if (tab.requiresOnline && !model.isConnected()) {
			Toast.makeText(getApplicationContext(), "Connect to a Server first!", Toast.LENGTH_SHORT).show();//TODO: R.string
			return true;
		}
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		
		ft
			.replace(android.R.id.content, tab.getFragment(), tab.getTag())
//			.addToBackStack(null)
			.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
			;
		ft.commit();
		
		currentTab = pos;
		sm.setSelectedView(getSelectedView(pos));
		return true;
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processNfcIntent(getIntent());
        }
		if (deferredLogin != null) {
			showLoginDialog(deferredLogin);
			deferredLogin = null;
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(Std.TAB, currentTab);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.activity_main, menu);
	    
        return true;
    }
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean act = model.isConnected();
		menu.findItem(R.id.menu_login).setEnabled(!act);
		menu.findItem(R.id.menu_logout).setEnabled(act);
		menu.findItem(R.id.menu_share_barcode).setEnabled(act);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			toggle();
			break;
		case R.id.menu_login:
			showLoginDialog(model.getLogin());
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
	
	private void showLoginDialog(Login login) {
		DialogFragment d = new LoginDialogFragment();
		Bundle b = new Bundle();
		b.putParcelable(Std.LOGIN, login);
		d.setArguments(b);
		d.show(getSupportFragmentManager(), Std.LOGIN_DIALOG);
	}
	
	/**
	 * Called if we e.g. Rotate the App
	 * We are saving the Model
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
		
		if (!keepModel)
			model.onDestroy();
		
		GCMRegistrar.onDestroy(this);
		
		super.onDestroy();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IntentIntegrator.REQUEST_CODE) {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
			if (scanResult == null) return;
			
			deferredLogin = Login.fromJSON(scanResult.getContents());
			
		} else if(requestCode == Std.PICK_IMAGE && data != null && data.getData() != null){
	        Uri uri = data.getData();

	        if (uri != null) {
	            //User had pick an image.
	            Cursor cursor = getContentResolver().query(uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
	            cursor.moveToFirst();

	            //Link to the image
	            final String imageFilePath = cursor.getString(0);
	            
	            Toast.makeText(getApplicationContext(), getResources().getString(R.string.picture_taken), Toast.LENGTH_SHORT).show();
	            Log.i(THIS, "Picture taken/selected: "+ imageFilePath);
	            
	            cursor.close();
	        }
	    }
		
		
	    super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}
	
	private void processNfcIntent(Intent intent) {
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		// only one message sent during the beam
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		// record 0 contains the MIME type, record 1 is the AAR, if present
		if (Std.APP_MIME.equals(new String(msg.getRecords()[0].getPayload()))) {
			if (model.isDisconnected()) {
				showLoginDialog(Login.fromJSON(new String(msg.getRecords()[2].getPayload())));
			} else {
				Toast.makeText(this, "Logout first!", Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, "Unknown NFC Beam received", Toast.LENGTH_LONG).show();
		}
		
	}
	
	/**
	 * IConnectionStatusListener
	 */
	@Override
	public void onConnectionStatusChange(ConnectionStatus status) {
		ActionBar ab = getSupportActionBar();
		switch (status) {//TODO: No Network on UI
		case Disconnecting:
			if (isOnlineTab()) {
				switchTabFallback();
			}
		case Connecting:
			activateProgressAnimation();
			break;
		case Unknown:
		case Disconnected:
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
		Toast.makeText(this, "Login Failed!", Toast.LENGTH_SHORT).show();
		
		onConnectionStatusChange(lastStatus);
	}
	
	/**
	 * LoginDialogFragment.IDialogCallback
	 */
	@Override
	public void onDialogPositiveClick(LoginDialogFragment dialog, Login login) {
		if (login.isComplete())
			model.login(login);
		else {
			Toast.makeText(this, getString(R.string.invalid), Toast.LENGTH_SHORT).show();
			showLoginDialog(login);
		}
	}

	@Override
	public void onDialogNegativeClick(LoginDialogFragment dialog) {
		
	}
	
	/**
	 * IModelActivity
	 */
	@Override
	public Model getModel() {
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
