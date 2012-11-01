package de.stadtrallye.rallyesoft;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.stadtrallye.rallyesoft.communications.PushService;
import de.stadtrallye.rallyesoft.fragments.OverviewFragment;

public class MainActivity extends SherlockFragmentActivity implements  ActionBar.OnNavigationListener {
	
	public PushService push;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(R.string.title_main);
//		setContentView(R.layout.overview);
		
		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(false);

		ActionBar.Tab tab = getSupportActionBar().newTab();
		
		Context context = ab.getThemedContext();
		ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.tabs, R.layout.sherlock_spinner_item);
//        list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		
		
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ab.setListNavigationCallbacks(list, this);
		ab.setSelectedNavigationItem(0);
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		switch (itemPosition) {
		case 0: 
			OverviewFragment newFragment = new OverviewFragment();
			
//		    ft.replace(R.id.content_frame, newFragment, null);
		    // Apply changes
//		    ft.commit();
		break;
		default:
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.unsupported_link), Toast.LENGTH_SHORT).show();
		break;
		}
		
		return false;
	}
}
