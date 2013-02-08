package de.stadtrallye.rallyesoft;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

import de.stadtrallye.rallyesoft.model.Model;

public class ImageViewActivity extends SherlockActivity {
	
	private ViewPager pager;
	private int chatroom;
	private int imgID;
	private Model model;
	private ImageAdapter adapter;
	private int[] images;
	private int startPos;
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
		setContentView(R.layout.activity_image_view);
		
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		model = Model.getInstance(this, false);
		
		pager = (ViewPager)findViewById(R.id.image_pager);
		Bundle b = getIntent().getExtras();
		chatroom = b.getInt(Std.CHATROOM);
		images = b.getIntArray(Std.IMAGE_LIST);
		startPos = b.getInt(Std.IMAGE);
        
		pager.setPageMargin(10);
		
        adapter = new ImageAdapter(images);
        pager.setAdapter(adapter);
        pager.setCurrentItem(startPos);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		getSupportActionBar().hide();
	}
	
	private class ImageAdapter extends PagerAdapter {

		private int[] images;
		private LayoutInflater inflater;
		private ImageLoader loader;

		public ImageAdapter(int[] images) {
			this.images = images;
			inflater = getLayoutInflater();
			
			loader = ImageLoader.getInstance();
			DisplayImageOptions disp = new DisplayImageOptions.Builder()
				.cacheOnDisc()
				.cacheInMemory() // Still unlimited Cache on Disk
				.showImageForEmptyUri(R.drawable.stub_image)
				.build();
			ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(ImageViewActivity.this)
//				.enableLogging()
				.defaultDisplayImageOptions(disp)
				.build();
	        loader.init(config);
		}
		
		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView((View) object);
		}
		
		@Override
		public Object instantiateItem(View view, int position) {
			final View imageLayout = inflater.inflate(R.layout.item_pager_image, null);
			final ImageView imageView = (ImageView) imageLayout.findViewById(R.id.image);
			final ProgressBar spinner = (ProgressBar) imageLayout.findViewById(R.id.loading);
			
			imageLayout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getSupportActionBar().isShowing())
						getSupportActionBar().hide();
					else
						getSupportActionBar().show();
				}
			});

			loader.displayImage(model.getUrlFromImageId(images[position], 's'), imageView, new SimpleImageLoadingListener() {
				@Override
				public void onLoadingStarted() {
					spinner.setVisibility(View.VISIBLE);
				}

				@Override
				public void onLoadingFailed(FailReason failReason) {
					String message = null;
					switch (failReason) {
						case IO_ERROR:
							message = "Input/Output error";
							break;
						case OUT_OF_MEMORY:
							message = "Out Of Memory error";
							break;
						case UNKNOWN:
							message = "Unknown error";
							break;
					}
					Toast.makeText(ImageViewActivity.this, message, Toast.LENGTH_SHORT).show();

					spinner.setVisibility(View.GONE);
					imageView.setImageResource(android.R.drawable.ic_delete);
				}

				@Override
				public void onLoadingComplete(Bitmap loadedImage) {
					spinner.setVisibility(View.GONE);
				}
			});

			((ViewPager) view).addView(imageLayout, 0);
			return imageLayout;
		}

		@Override
		public int getCount() {
			return images.length;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
		
		
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
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
