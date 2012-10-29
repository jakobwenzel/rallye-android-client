package de.stadtrallye.rallyesoft;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import de.stadtrallye.rallyesoft.R;

import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class OverviewActivity extends FragmentActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        setTitle(R.string.title_overview);
		setContentView(R.layout.overview);
//		setBehindContentView(R.layout.dashboard_main);
//		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		
//		SlidingMenu sm = getSlidingMenu();
//		sm.setShadowWidthRes(R.dimen.shadow_width);
//		sm.setShadowDrawable(R.drawable.shadow);
//		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
//		
//		ListView dashboard = (ListView) sm.findViewById(R.id.dashboard_list);
//        ArrayAdapter<String> dashAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, getResources().getStringArray(R.array.dashboard_entries));
//        dashboard.setAdapter(dashAdapter);
	}
	
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//		case android.R.id.home:
//			toggle();
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}

}
