package de.stadtrallye.rallyesoft;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
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

public class ImageViewActivity extends SherlockActivity implements OnTouchListener {
	
	private static final String THIS = ImageViewActivity.class.getSimpleName();
	
	private ViewPager pager;
//	private int chatroom;
//	private int imgID;
	private Model model;
	private ImageAdapter adapter;
	private int[] images;
	private int startPos;	
//	private ScaleGestureDetector detector;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
		setContentView(R.layout.activity_image_view);
		findViewById(R.id.image_pager).setOnTouchListener(this);
		
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		model = Model.getInstance(this, false);
		
		pager = (ViewPager)findViewById(R.id.image_pager);
		
		Bundle b = getIntent().getExtras();
//		chatroom = b.getInt(Std.CHATROOM);
		images = b.getIntArray(Std.IMAGE_LIST);
		startPos = b.getInt(Std.IMAGE);
        
		pager.setPageMargin(10);
		
        adapter = new ImageAdapter(images);
        pager.setAdapter(adapter);
        pager.setCurrentItem(startPos);
        
//        findViewById(R.id.image_pager).setOnTouchListener(this);
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
	
	private PointF begin = new PointF();
	private PointF p2 = new PointF();
	private Matrix mBase = new Matrix();
	private Matrix mScale = new Matrix();
	private enum Mode { None, Drag, Zoom };
	private Mode mode = Mode.None;

	@Override
	public boolean onTouch(View v, MotionEvent e) {
		
		switch (e.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			mScale.preScale(2, 2);
			mode = Mode.Drag;
			begin = new PointF(e.getX(), e.getY());
			break;
		case MotionEvent.ACTION_UP:
			mode = Mode.None;
//			begin = null;
			break;
		case MotionEvent.ACTION_MOVE:
			mBase.set(mScale);
			mBase.postTranslate(e.getX() - begin.x, e.getY() - begin.y);
			break;
		default:
			Log.w(THIS, "Unhandled Action: ");
			dumpEvent(e);
		}
		
		
		
		ImageView img = (ImageView) pager.getChildAt(pager.getCurrentItem()).findViewById(R.id.image);
		img.setImageMatrix(mBase);
		
		return false;
	}
	
	
	
	private void dumpEvent(MotionEvent event) {
		String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
				"POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event ACTION_").append(names[actionCode]);
		if (actionCode == MotionEvent.ACTION_POINTER_DOWN
				|| actionCode == MotionEvent.ACTION_POINTER_UP) {
			sb.append("(pid ").append(
					action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")");
		}
		sb.append("[");
		for (int i = 0; i < event.getPointerCount(); i++) {
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if (i + 1 < event.getPointerCount())
				sb.append(";");
		}
		sb.append("]");
		Log.d(THIS, sb.toString());
	}

}
