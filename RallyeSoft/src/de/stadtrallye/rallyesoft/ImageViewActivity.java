package de.stadtrallye.rallyesoft;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
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

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.IPictureGallery;
import de.stadtrallye.rallyesoft.model.IPictureGallery.Size;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.widget.GalleryPager;

public class ImageViewActivity extends SherlockActivity {
	
	private static final String THIS = ImageViewActivity.class.getSimpleName();
	
	private GalleryPager pager;
	private Model model;
	private ImageAdapter adapter;

	private IChatroom chatroom;
	private IPictureGallery gallery;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
		setContentView(R.layout.activity_image_view);
		
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		
		pager = (GalleryPager)findViewById(R.id.image_pager);
		pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin));
		
		model = Model.getInstance(getApplicationContext());
		
		Bundle b = getIntent().getExtras();
		chatroom = model.getChatroom(b.getInt(Std.CHATROOM));
		gallery = chatroom.getPictureGallery(b.getInt(Std.IMAGE));
		
        adapter = new ImageAdapter();
        pager.setAdapter(adapter);
        pager.setCurrentItem(gallery.getInitialPosition());
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		getSupportActionBar().hide();
	}
	
	private class ImageAdapter extends PagerAdapter {

		private LayoutInflater inflater;
		private ImageLoader loader;

		public ImageAdapter() {
			inflater = getLayoutInflater();
			
			loader = ImageLoader.getInstance();
			DisplayImageOptions disp = new DisplayImageOptions.Builder()
				.cacheOnDisc()
				.cacheInMemory() // Still unlimited Cache on Disk
//				.showImageForEmptyUri(R.drawable.stub_image)
				.build();
			ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(ImageViewActivity.this)
//				.enableLogging()
				.defaultDisplayImageOptions(disp)
				.build();
	        loader.init(config);
		}
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}
		
		@Override
		public Object instantiateItem(ViewGroup view, int position) {
			final View imageLayout = inflater.inflate(R.layout.item_pager_image, null);
			final ImageView imageView = (ImageView) imageLayout.findViewById(R.id.image);
			final ProgressBar spinner = (ProgressBar) imageLayout.findViewById(R.id.loading);
			
			imageLayout.setClickable(true);
			imageLayout.setOnTouchListener(new TouchControl(imageView));

			loader.displayImage(gallery.getPictureUrl(position), imageView, new SimpleImageLoadingListener() {
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
			return gallery.size();
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_image_view, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean small = (gallery.getImageSize() == Size.Small);
		
		menu.findItem(R.id.image_level_large).setEnabled(small);
		menu.findItem(R.id.image_level_small).setEnabled(!small);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.image_level_large:
			gallery.setImageSize(Size.Large);
			pager.setAdapter(adapter);
			break;
		case R.id.image_level_small:
			gallery.setImageSize(Size.Small);
			pager.setAdapter(adapter);
			break;
		}
		return false;
	}
	
	public void toggleActionBar() {
		if (getSupportActionBar().isShowing())
			getSupportActionBar().hide();
		else
			getSupportActionBar().show();
	}
	
	private enum Mode { None, Drag, Zoom };
	
	private class TouchControl extends SimpleOnGestureListener implements OnTouchListener {
	
		private PointF pLast = new PointF();
		
		private GestureDetector tap = new GestureDetector(getApplicationContext(), this);
		
		private PointF pImg;
		private PointF pView;
		
		private Matrix mBase = new Matrix();
		private Matrix mCurrent = new Matrix();
		
		private float dLast;
		private float dCurrent;
		
		private float baseScale = 1;
		private float currentScale = 1;
		private static final float maxScale = 5;
		
		private boolean widthLimit;
		
		private Mode mode = Mode.None;
		private ImageView img;
		
		
		public TouchControl(ImageView img) {
			this.img = img;
		}
		
		@Override
		public boolean onTouch(View v, MotionEvent e) {
			tap.onTouchEvent(e);
			
			switch (e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				mode = Mode.Drag;
//				Log.d(THIS, "Drag");
				
				if (pImg == null) {
					mBase.set(img.getImageMatrix());
					pImg = new PointF(img.getDrawable().getIntrinsicWidth(), img.getDrawable().getIntrinsicHeight());
					pView = new PointF(img.getWidth(), img.getHeight());
					widthLimit = ((pView.x / pView.y) < (pImg.x / pImg.y));
					
					mCurrent.set(mBase);
						
					currentScale = baseScale = mBase.mapRadius(1);
				}
				
				pLast.set(e.getX(), e.getY());
//				pDown.set(pLast);
//				tDown = System.currentTimeMillis();
				return true;
			case MotionEvent.ACTION_UP:
				mode = Mode.None;

//				if (distance(pDown, e) <= clickMargin && System.currentTimeMillis()-tDown <= timeMargin) {
//					toggleActionBar();
//				}
				
				return true;
			case MotionEvent.ACTION_POINTER_DOWN:
				mode = Mode.Zoom;
//				Log.d(THIS, "scaling, blocking pager");
				pager.setInterceptTouch(false);
//				Log.d(THIS, "Zoom");
				
				dLast = distance(e);
				pLast = midPoint(e);
				return true;
			case MotionEvent.ACTION_POINTER_UP:
				mode = Mode.Drag;
				
				if (currentScale <= baseScale) {
					pager.setInterceptTouch(true);
				}
				int i = (e.getActionIndex() == 1)? 0 : 1; // multi touch proof, works if more than 2 fingers were on the screen (worst case: jump)
				pLast.set(e.getX(i), e.getY(i));
				return true;
			case MotionEvent.ACTION_MOVE:
				if (mode == Mode.Drag && currentScale == baseScale)
					return false;
				
				PointF pCurrent;
				
				if (mode == Mode.Zoom) {
					pCurrent = midPoint(e);
					dCurrent = distance(e);
					
					float s = dCurrent / dLast;
					dLast = dCurrent;
					float newScale = currentScale * s;
//					Log.d(THIS, "currentScale: "+ currentScale +" , newScale: "+ newScale);
					
					if (newScale <= baseScale) {
						s = baseScale / currentScale;
						currentScale = baseScale;
					} else if (newScale > maxScale) {
						s = maxScale / currentScale;
						currentScale = maxScale;
					} else { // maybe margin for error?
						currentScale = newScale;
					}
					
					
					mCurrent.postScale(s, s, getFocal(pImg.x, pView.x, pLast.x), getFocal(pImg.y, pView.y, pLast.y));
				} else {
					pCurrent = new PointF(e.getX(), e.getY());
				}
				
				PointF d = new PointF(pCurrent.x - pLast.x, pCurrent.y - pLast.y);
				
				mCurrent.postTranslate(d.x, d.y);
				
				correctTransform();
				
				pLast.set(pCurrent);
				
				if (img.getScaleType() != ScaleType.MATRIX)
					img.setScaleType(ScaleType.MATRIX);
				
				img.setImageMatrix(mCurrent);
				return true;
			default:
				Log.w(THIS, "Unhandled Action: ");
				dumpEvent(e);
				return false;
			}
		}
		
		private float getFocal(float size, float available, float user) {
			return (size * currentScale >= available)? user : available / 2;
		}
		
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			float s;
			if (currentScale <= maxScale /3) {
				s = maxScale / (currentScale * 2);
				currentScale = maxScale / 2;
				mCurrent.postScale(s, s, e.getX(), e.getY());
			} else {
				s = baseScale / currentScale;
				currentScale = baseScale;
				mCurrent.set(mBase);
			}
			correctTransform();
			img.setImageMatrix(mCurrent);
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			toggleActionBar();
			return true;
		}
		
		private void correctTransform() {//  b/2 -a/2
			float[] t = new float[]{0, 0, pImg.x, pImg.y};
			float[] v = new float[]{0,0};
			float[] max = new float[]{pView.x, pView.y};
			mCurrent.mapPoints(t);
			int prim, sec;
			int primC = 0;
			
			prim = (widthLimit)? 0 : 1;
			sec = (widthLimit)? 1: 0;
			
			if (t[prim] > 0) {
				v[prim] = -t[prim];
				primC += 1;
			}
			if (t[prim+2] < max[prim]) {
				primC += 1;
				v[prim] = max[prim] - t[prim+2];
			}
			
			boolean center = (t[sec+2] - t[sec] < max[sec]);
				
			if (t[sec] > 0) {
				v[sec] = (center)? -t[sec]/2 : -t[sec];
			}
			if (t[sec+2] < max[sec]) {
				v[sec] += (center)? (max[sec] - t[sec+2])/2 : max[sec] - t[sec+2];
			}
			
			if (primC > 1) {
				Log.w(THIS, "conflicting inside bounds => scale problem");
			} else {
				mCurrent.postTranslate(v[0], v[1]);
			}
		}
		
		private float distance(MotionEvent e) {
			return distance(e.getX(0), e.getY(0), e.getX(1), e.getY(1));
		}
//		private float distance(PointF a, MotionEvent b) {
//			return distance(a.x, a.y, b.getX(), b.getY());
//		}
		private float distance(float ax, float ay, float bx, float by) {
			float a = ax - bx;
			float b = ay - by;
			return (float) Math.sqrt(a*a + b*b);
		}
		
		private PointF midPoint(MotionEvent e) {
			return new PointF((e.getX(0)+e.getX(1))/2, (e.getY(0)+e.getY(1))/2);
		}
		
		private void dumpEvent(MotionEvent event) {

			String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
					"POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
			StringBuilder sb = new StringBuilder();
//			int action = event.getAction();
			int actionCode = event.getActionMasked();
			sb.append("event ACTION_").append(names[actionCode]);
			if (actionCode == MotionEvent.ACTION_POINTER_DOWN
					|| actionCode == MotionEvent.ACTION_POINTER_UP) {
				sb.append("(pid ").append(event.getActionIndex());
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
}
