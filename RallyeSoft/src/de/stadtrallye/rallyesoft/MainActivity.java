package de.stadtrallye.rallyesoft;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

import de.stadtrallye.rallyesoft.fragments.ChatsFragment;
import de.stadtrallye.rallyesoft.fragments.LoginDialogFragment;
import de.stadtrallye.rallyesoft.fragments.OverviewFragment;
import de.stadtrallye.rallyesoft.model.IConnectionStatusListener;
import de.stadtrallye.rallyesoft.model.IModel.ConnectionStatus;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.comm.PushInit;

public class MainActivity extends SlidingFragmentActivity implements  ActionBar.OnNavigationListener, AdapterView.OnItemClickListener,
																		LoginDialogFragment.IDialogCallback, IModelActivity,
																		IConnectionStatusListener, IProgressUI {
	
	private static final String THIS = MainActivity.class.getSimpleName();
	
	public PushInit push;
	private Model model;
	private boolean progressCircle = false;
//	private Fragment currentFragment;
	private int lastTab = 0;
	private SharedPreferences config;
	private ArrayList<FragmentHandler<?>> tabs;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Titel und Inhalt + SideBar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		    
		setTitle(R.string.title_main);
		setContentView(R.layout.main);
		setBehindContentView(R.layout.dashboard_main);
		
		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(true);
		
		// Settings for SideBar
		SlidingMenu sm = getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.defaultshadow);
//		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setBehindWidthRes(R.dimen.slidingmenu_width);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		
		// Set last tab if any
		int tabIndex = 0;
		boolean loggedIn = false;
		if (savedInstanceState != null) {
			tabIndex = savedInstanceState.getInt("tabIndex");
			loggedIn = savedInstanceState.getBoolean("loggedIn");
		}
		
		// Ray's INIT
		PushInit.ensureRegistration(this);
		config = getSharedPreferences(getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
		model = Model.getInstance(this, config, loggedIn);
		model.addListener(this);
		if (!loggedIn)
			model.checkConnectionStatus();
		
		
        // Populate SideBar
        ListView dashboard = (ListView) sm.findViewById(R.id.dashboard_list);
        ArrayAdapter<String> dashAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, getResources().getStringArray(R.array.dashboard_entries));
        dashboard.setAdapter(dashAdapter);
        dashboard.setOnItemClickListener(this);
		
        // Populate tabs
		Context context = ab.getThemedContext();
		ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.tabs, R.layout.sherlock_spinner_item);
        list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ab.setListNavigationCallbacks(list, this);
		
        //Force the Menu SoftButton even if hardware button present (only for 4.0 and greater)
		getOverflowMenu();
		
		
		//Create FragmentHandlers
		tabs = new ArrayList<FragmentHandler<?>>();
		tabs.add(new FragmentHandler<OverviewFragment>("overview", OverviewFragment.class, null));
		tabs.add(null);
		tabs.add(null);
		tabs.add(new FragmentHandler<ChatsFragment>("chat", ChatsFragment.class, null));
		
		getSupportActionBar().setSelectedNavigationItem(tabIndex);
		
		
		//DEBUG
//		ChatsFragment.enableDebugLogging();
	}
	
	/**
	 * Envelops a Fragment, reuses a already existing Fragment otherwise instantiates a new one
	 * @author Ramon
	 *
	 * @param <T> Fragment Type to envelop
	 */
	private class FragmentHandler<T extends Fragment> {
		
//		public static final boolean RE_USE = true;
//		public static final boolean NEW = false;
		
		private String tag;
		private Class<T> clz;
//		private Fragment fragment;
		private Bundle arg;
//		private boolean reUse;

		public FragmentHandler(String tag, Class<T> clz, Bundle arg) {
			this.tag = tag;
			this.clz = clz;
			this.arg = arg;
//			this.reUse = reUse;
		}
		
		public Fragment getFragment() {
			Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
			
			if (f == null) {
				f = Fragment.instantiate(MainActivity.this, clz.getName());
				
				if (arg != null)
					f.setArguments(arg);
			}
			return f;
		}
		
		public String getTag() {
			return tag;
		}
		
		public Bundle getArguments() {
			return arg;
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		setSupportProgressBarIndeterminateVisibility(false);
	}
	
	private void getOverflowMenu() {
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
	 * AdapterView.OnItemClickListener
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		if (lastTab != pos)
		{
			getSupportActionBar().setSelectedNavigationItem(pos);
		}
		getSlidingMenu().showContent();
	}
	
	/**
	 * ActionBar.OnNavigationListener
	 */
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		return onSwitchTab(itemPosition, itemId);
	}
	
	private boolean onSwitchTab(int pos, long id) {
		
		switch (pos) {
		case 1:
//			newFragment = new MapFragment();
			Intent i = new Intent(this, GameMapActivity.class);
//			i.putExtra("pull", pull);
			startActivity(i);
		return true;
		case 2:
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.unsupported_link), Toast.LENGTH_SHORT).show();
		return false;
		}
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		FragmentHandler<?> tab = tabs.get(pos);
		ft
			.replace(R.id.content_frame, tab.getFragment(), tab.getTag())
//			.addToBackStack(null)
			;
		ft.commit();
		
		lastTab = pos;
		return true;
	}
	
	@Override
	public void onBackPressed() {
		
//		if (getSlidingMenu().isMenuShowing())
			super.onBackPressed(); //TODO: Either put Fragments on Backstack or close when in SlidingMenu and Back is pressed
//		else
//			getSlidingMenu().showMenu();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tabIndex", getSupportActionBar().getSelectedNavigationIndex());
		outState.putBoolean("loggedIn", model.isLoggedIn());
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.activity_main, menu);
	    
        return true;
    }
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		boolean act = model.isLoggedIn();
		menu.findItem(R.id.menu_login).setEnabled(!act);
		menu.findItem(R.id.menu_logout).setEnabled(act);
		Log.d("MainActivity", "isLoggedIn(): " +act);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			toggle();
			break;
//		case R.id.menu_foto:
//			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
////		    Uri fileUri =
////		    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
//		    startActivityForResult(intent, 100);
//		    break;
		case R.id.menu_login:
			DialogFragment d = new LoginDialogFragment();
			Bundle b = new Bundle();
			b.putString("server", model.getServer());
			b.putInt("group", model.getGroupId());
			b.putString("password", model.getPassword());
			d.setArguments(b);
			d.show(getSupportFragmentManager(), "loginDialog");
			break;
		case R.id.menu_logout:
			model.logout();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onDestroy() {
		Log.d("MainActivity", "Destroying...");
		
		model.onDestroy();
		
		GCMRegistrar.onDestroy(this);
		
		super.onDestroy();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Toast.makeText(getApplicationContext(), getResources().getString(R.string.picture_taken), Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * IConnectionStatusListener
	 */
	@Override
	public void onConnectionStatusChange(ConnectionStatus status) {
		ActionBar ab = getSupportActionBar();
		switch (status) {
		case Connected:
			deactivateProgressAnimation();
			ab.setSubtitle(R.string.connected);
			Log.i("MainActivity", "Logged in!");
			Toast.makeText(this, getResources().getString(R.string.login), Toast.LENGTH_SHORT).show();
			break;
		case Disconnected:
			deactivateProgressAnimation();
			ab.setSubtitle(R.string.notConnected);
			Log.i("MainActivity", "Logged out!");
			Toast.makeText(this, getResources().getString(R.string.logout), Toast.LENGTH_SHORT).show();
			break;
		case Disconnecting:
		case Connecting:
			activateProgressAnimation();
			//TODO: show message...
			break;
		case NoNetwork:
			//TODO: No Network on UI
		}
	}
	
	@Override
	public void onConnectionFailed(Exception e, ConnectionStatus lastStatus) {
		Log.i("MainActivity", "Login failed!");
		Toast.makeText(this, "Login Failed!", Toast.LENGTH_SHORT).show();
		
		onConnectionStatusChange(lastStatus);
	}
	
	/**
	 * LoginDialogFragment.IDialogCallback
	 */
	@Override
	public void onDialogPositiveClick(LoginDialogFragment dialog, String server, int group, String pw) {
		model.login(server, pw, group);
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
