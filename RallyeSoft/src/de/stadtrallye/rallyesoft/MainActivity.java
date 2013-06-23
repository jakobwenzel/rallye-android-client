package de.stadtrallye.rallyesoft;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.fragments.ChatsFragment;
import de.stadtrallye.rallyesoft.fragments.GameMapFragment;
import de.stadtrallye.rallyesoft.fragments.OverviewFragment;
import de.stadtrallye.rallyesoft.fragments.TurnFragment;
import de.stadtrallye.rallyesoft.fragments.WelcomeFragment;
import de.stadtrallye.rallyesoft.model.IConnectionStatusListener;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.IModel.ConnectionStatus;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.LatLngAdapter;
import de.stadtrallye.rallyesoft.net.NfcCallback;
import de.stadtrallye.rallyesoft.net.PushInit;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.IProgressUI;

public class MainActivity extends SlidingFragmentActivity implements  ActionBar.OnNavigationListener, AdapterView.OnItemClickListener, IModelActivity,
																		IConnectionStatusListener, IProgressUI {
	
	private static final String THIS = MainActivity.class.getSimpleName();

	private IModel model;
	private boolean keepModel = false;
	private boolean progressCircle = false;
	private int currentTab;
	private String[] nav;
	private ArrayList<FragmentHandler<?>> tabs;

	private FragmentHandler<GameMapFragment> mapFragmentHandler;

	private SlidingMenu sm;
	private boolean forceRefreshFragments;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		initFrame();
		
		initSlidingMenu();

		// Ray's INIT
		PushInit.ensureRegistration(this);
		model = (Model) getLastCustomNonConfigurationInstance();
		if (model == null)
			model = Model.getModel(getApplicationContext());
		model.addListener(this);
		keepModel = false;
		
		// Recover Last State
		if (savedInstanceState != null) {
			currentTab = savedInstanceState.getInt(Std.TAB, 1);
		} else {
			currentTab = (model.isEmpty())? 0 : 1;
		}
		
		forceOverflowMenu();
		
		initFragments();
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			initNFC();
		}
		
	}
	
	private ActionBar initFrame() {
		// Title and Content
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
        ArrayAdapter<String> dashAdapter = new ArrayAdapter<>(this, R.layout.dashboard_item, android.R.id.text1, nav);
        dashboard.setAdapter(dashAdapter);
        dashboard.setOnItemClickListener(this);
	}
	
	private void initFragments() {
		//Create FragmentHandlers
		tabs = new ArrayList<>();
		tabs.add(new FragmentHandler<>("welcome", WelcomeFragment.class, false));
		tabs.add(new FragmentHandler<>("overview", OverviewFragment.class, false));
		tabs.add(mapFragmentHandler = new FragmentHandler<>("map", GameMapFragment.class, false));
		tabs.add(new FragmentHandler<>("turn", TurnFragment.class, false));
		tabs.add(new FragmentHandler<>("chat", ChatsFragment.class, true));
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

		private final String tag;
		private final Class<T> clz;
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

		if (forceRefreshFragments) {
			switchTab(0);
			currentTab = 1;
			forceRefreshFragments = false;
		}

		switchTab(currentTab);

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
			switchTab(pos);
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
		switchTab(currentTab = 1);
	}

	/**
	 * ActionBar.OnNavigationListener
	 */
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		return switchTab(itemPosition);
	}
	
	private boolean switchTab(int pos) {
		
		switch (pos) {
		case 2://Map
			Bundle b = new Bundle();
			GoogleMapOptions gmo = new GoogleMapOptions().compassEnabled(true);
			de.rallye.model.structures.LatLng loc = model.getMap().getMapLocation();
			if (loc != null) {
				gmo.camera(new CameraPosition(LatLngAdapter.toGms(loc), model.getMap().getZoomLevel(), 0, 0));
			}
			b.putParcelable("MapOptions", gmo);
			mapFragmentHandler.setArguments(b);
			break;
		default:
			if (pos < 0 || pos > 4) {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.unsupported_link), Toast.LENGTH_SHORT).show();
				return false;
			}
		}

		setTitle(nav[pos]);
		
		FragmentHandler<?> tab = tabs.get(pos);
		
		if (tab.requiresOnline && !model.isConnected()) {
			Toast.makeText(getApplicationContext(), getString(R.string.need_connection), Toast.LENGTH_SHORT).show();
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
//		menu.findItem(R.id.menu_login).setEnabled(!act);
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
				forceRefreshFragments = true;
			}
		} else if(data != null){
	        Uri uri = data.getData();

	        if (uri != null) {
				try {
					//User has picked an image.
					Cursor cursor = getContentResolver().query(uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
					cursor.moveToFirst();

					//Link to the image
					final String imageFilePath = cursor.getString(0);

					Log.i(THIS, "Picture taken/selected: "+ imageFilePath);

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
	 * IConnectionStatusListener
	 */
	@Override
	public void onConnectionStatusChange(ConnectionStatus status) {
		ActionBar ab = getSupportActionBar();
		switch (status) {//TODO: Add "No Network" status to UI (requires Model to have a "No Network" status)
		case Disconnecting:
			if (isOnlineTab()) {
				switchTabFallback();
			}
		case Connecting:
			activateProgressAnimation();
			break;
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
		Toast.makeText(this, getString(R.string.connection_failure), Toast.LENGTH_SHORT).show();
		
		onConnectionStatusChange(lastStatus);
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
