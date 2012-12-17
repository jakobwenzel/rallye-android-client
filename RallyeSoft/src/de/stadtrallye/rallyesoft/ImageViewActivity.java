package de.stadtrallye.rallyesoft;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import de.stadtrallye.rallyesoft.model.Model;

public class ImageViewActivity extends SherlockActivity {
	
	private ImageView img;
	private int chatroom;
	private int imgID;
	private SharedPreferences config;
	private Model model;
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_view);
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		config = getSharedPreferences(getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
		model = new Model(this, config);
		
		img = (ImageView)findViewById(R.id.image);
		Bundle b = getIntent().getExtras();
		chatroom = b.getInt(Config.CHATROOM);
		imgID = b.getInt(Config.IMAGE);
		
		ImageLoader loader = ImageLoader.getInstance();
		DisplayImageOptions disp = new DisplayImageOptions.Builder()
			.cacheOnDisc()
			.cacheInMemory() // Still unlimited Chache on Disk
			.showStubImage(R.drawable.stub_image)
			.build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
//			.enableLogging()
			.defaultDisplayImageOptions(disp)
			.build();
        loader.init(config);
        
        loader.displayImage(model.getImageUrl(imgID, 's'), img);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.activity_image_view, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
