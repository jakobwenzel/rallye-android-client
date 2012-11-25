package de.stadtrallye.rallyesoft;

import java.lang.reflect.Field;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

import de.stadtrallye.rallyesoft.communications.PushService;
import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.fragments.ChatFragment;
import de.stadtrallye.rallyesoft.fragments.LoginDialogFragment;
import de.stadtrallye.rallyesoft.fragments.OverviewFragment;

public class MainActivity extends SlidingFragmentActivity implements  ActionBar.OnNavigationListener, AdapterView.OnItemClickListener {
	
	public PushService push;
	public RallyePull pull;
	private Fragment currentFragment;
	private int lastTab = 0;
	private SharedPreferences config;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Avoid Handler Exception
		new AsyncTask<Void, Void, Void>() {
		      @Override
		      protected Void doInBackground(Void... params) {
		        return null;
		      }
		    }.execute();
		
		// Titel und Inhalt + SideBar
		setTitle(R.string.title_main);
		setContentView(R.layout.main);
		setBehindContentView(R.layout.dashboard_main);
		
		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);

//		ActionBar.Tab tab = getSupportActionBar().newTab();
		
		// Settings for SideBar
		SlidingMenu sm = getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.defaultshadow);
//		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setBehindWidthRes(R.dimen.slidingmenu_width);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		
//		sm.setBehindCanvasTransformer(new CanvasTransformer() {
//			public void transformCanvas(Canvas canvas, float percentOpen) {
//				float scale = (float) (percentOpen*0.25 + 0.75);
//				canvas.scale(scale, scale, canvas.getWidth()/2, canvas.getHeight()/2);
//			}
//		});
		
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
		
		getOverflowMenu();
		
		// Ray's INIT
		PushService.ensureRegistration(this);
		config = getSharedPreferences(getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
		pull = new RallyePull(config, this.getApplicationContext());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
//		if (config.getBoolean("firstLaunch", true)) {
//			new LoginDialogFragment(config).show(getSupportFragmentManager(), "loginDialog");
//		}
		
		
		getSupportActionBar().setSelectedNavigationItem(lastTab);
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
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		if (lastTab != pos)
		{
//			onSwitchTab(pos, id);
			getSupportActionBar().setSelectedNavigationItem(pos);
		}
		getSlidingMenu().showAbove();
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		return onSwitchTab(itemPosition, itemId);
	}
	
	private boolean onSwitchTab(int pos, long id) {
		
		Fragment newFragment = null;
		
		switch (pos) {
		case 0: 
			newFragment = new OverviewFragment();
		break;
		case 1:
//			newFragment = new MapFragment();
			Intent i = new Intent(this, GameMapActivity.class);
//			i.putExtra("pull", pull);
			startActivityFromFragment(currentFragment, i, -1);
		return true;
		case 3:
			newFragment = new ChatFragment();
			break;
		default:
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.unsupported_link), Toast.LENGTH_SHORT).show();
		return false;
		}
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.content_frame, newFragment);
		ft.commit();
		currentFragment = newFragment;
		
		lastTab = pos;
		return true;
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.activity_main, menu);
	    
//        menu.add("Save")
//            .setIcon(R.drawable.ic_compose)
//            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		

//        menu.add("Search")
//            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//
//        menu.add("Refresh")
//            .setIcon(R.drawable.ic_refresh_inverse)
//            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			toggle();
			break;
		case R.id.menu_foto:
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//		    Uri fileUri =
//		    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
		    startActivityForResult(intent, 100);
		    break;
		case R.id.menu_logon:
			new LoginDialogFragment(config).show(getSupportFragmentManager(), "loginDialog");
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Toast.makeText(getApplicationContext(), getResources().getString(R.string.picture_taken), Toast.LENGTH_SHORT).show();
	}
}
