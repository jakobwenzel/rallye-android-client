package de.stadtrallye.rallyesoft;

import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

import de.stadtrallye.rallyesoft.communications.PushService;

public class MainActivity extends SherlockActivity {
	
	public PushService push;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(R.string.title_main);
		setContentView(R.layout.main);
		
		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(false);
		ab.addTab(tab
	}
}
