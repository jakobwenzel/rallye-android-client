package de.stadtrallye.rallyesoft;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
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

import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.IPictureGallery;
import de.stadtrallye.rallyesoft.model.Model;

public class ImageViewActivity extends SherlockActivity implements OnClickListener {
	
	private static final String THIS = ImageViewActivity.class.getSimpleName();
	
	private ViewPager pager;
	private Model model;
	private ImageAdapter adapter;
	
	private TouchFilter touchFilter = new TouchFilter();

	private IChatroom chatroom;
	private IPictureGallery gallery;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
		setContentView(R.layout.activity_image_view);
		
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		model = Model.getInstance(this, true);
		
		pager = (ViewPager)findViewById(R.id.image_pager);
		pager.setOnTouchListener(touchFilter);
		
		Bundle b = getIntent().getExtras();
		chatroom = model.getChatroom(b.getInt(Std.CHATROOM));
		gallery = chatroom.getPictureGallery(b.getInt(Std.IMAGE));
        
		pager.setPageMargin(10);
		
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
		public void setPrimaryItem(ViewGroup container, int position, Object object) {
//			try {
//				
//				ImageView img = (ImageView)((View)object).findViewById(R.id.image);
//				if (currentImg != null && currentImg != img)
//					currentImg.setScaleType(ScaleType.FIT_CENTER);
//				currentImg = img;
//				
////				Log.d(THIS, "Pager: "+position+": "+ currentImg);
//			} catch (Exception e) {
//				Log.e(THIS, "no image", e);
//			}
			super.setPrimaryItem(container, position, object);
		}
		
		@Override
		public Object instantiateItem(View view, int position) {
			final View imageLayout = inflater.inflate(R.layout.item_pager_image, null);
			final ImageView imageView = (ImageView) imageLayout.findViewById(R.id.image);
			final ProgressBar spinner = (ProgressBar) imageLayout.findViewById(R.id.loading);
			
			imageLayout.setOnClickListener(ImageViewActivity.this);
			imageLayout.setOnTouchListener(new Pincher(imageView));

			loader.displayImage(gallery.getPictureUrl(position, 's'), imageView, new SimpleImageLoadingListener() {
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
//		getMenuInflater().inflate(R.menu.activity_image_view, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onClick(View v) {
		if (getSupportActionBar().isShowing())
			getSupportActionBar().hide();
		else
			getSupportActionBar().show();
	}
	
	private enum Mode { None, Drag, Zoom };
	
	private class Pincher implements OnTouchListener {
	
		private PointF pBegin = new PointF();
		private PointF vTrans = new PointF();
		private Matrix mBase = new Matrix();
		private Matrix mBegin = new Matrix();
		private Matrix mCurrent = new Matrix();
		private float dBegin;
		private PointF pImg;
		private PointF pBox;
		
		private float baseScale = 1;
		private float beginScale = 1, currentScale = 1;
		private boolean block = true;
		
		private Mode mode = Mode.None;
		private ImageView img;
		
		
		public Pincher(ImageView img) {
			this.img = img;
		}
		
		@Override
		public boolean onTouch(View v, MotionEvent e) {
			switch (e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				mode = Mode.Drag;
				Log.d(THIS, "Drag");
				
				if (pImg == null) {
					mBase.set(img.getImageMatrix());
					pImg = new PointF(img.getDrawable().getIntrinsicWidth(), img.getDrawable().getIntrinsicHeight());
					pBox = new PointF(img.getWidth(), img.getHeight());
					currentScale = beginScale = baseScale = mBase.mapRadius(1);
				}
				
//				mBegin.set(img.getImageMatrix());
				pBegin.set(e.getX(), e.getY());
				return false;
			case MotionEvent.ACTION_UP:
				mode = Mode.None;
				return false;
			case MotionEvent.ACTION_POINTER_DOWN:
				mode = Mode.Zoom;
				Log.d(THIS, "Zoom");
				
//				mBegin.set(mCurrent);
				dBegin = distance(e);
				pBegin = midPoint(e);
				return false;
			case MotionEvent.ACTION_POINTER_UP:
				mode = Mode.Drag;
				
//				mBegin.set(mCurrent);
				beginScale = currentScale;
				pBegin.set(e.getX(), e.getY());
				return false;
			case MotionEvent.ACTION_MOVE:
//				if (mode == Mode.Drag && beginScale == baseScale)
//					return false;
				
				mCurrent.set(mBase);
				
				PointF d;
				
				if (mode == Mode.Zoom) {
					float s = distance(e) / dBegin;
//					if (beginScale * s < baseScale) {
//						Log.d(THIS, "blocked scale down: "+ s);
//						s = baseScale;
//					}
					currentScale = s * beginScale;
					
					d = midPoint(e);
				} else {
					d = new PointF(e.getX(), e.getY());
				}
				
				mCurrent.postScale(currentScale, currentScale, pBegin.x, pBegin.y);
				mCurrent.postTranslate(d.x - pBegin.x, d.y - pBegin.y);
				
				correctTransform();
				
				
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
		
		private void correctTransform() {
			float[] t = new float[]{0, 0};
			mCurrent.mapPoints(t);
			PointF v = new PointF();
			boolean trans = false, scale = false;
			
			if (t[0] > 0) {
				v.x = -t[0];
				trans = true;
			}
			if (t[1] > 0) {
				v.y = -t[1];
				trans = true;
			}
			t = new float[]{pImg.x, pImg.y};
			mCurrent.mapPoints(t);
			
			if (t[0] < pBox.x) {
				if (trans) {
					scale = true;
				} else
					v.x = t[0];
			}
			if (t[1] < pBox.y) {
				if (trans) {
					scale = true;
				} else
					v.y = t[1];
			}
			
			if (!scale && trans) {
				Log.d(THIS, "blocked translation inside bounds");
				mCurrent.postTranslate(v.x, v.y);
			} else if (scale) {
				Log.d(THIS, "blocked scaling inside bounds");
				mCurrent.set(mBase);
				currentScale = baseScale;
			}
		}
		
		public boolean onTouch2(View v, MotionEvent e) {
			
			switch (e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				mode = Mode.Drag;
				
				mBase.set(img.getImageMatrix());
				mCurrent.set(img.getImageMatrix());
				
				
				pBegin = new PointF(e.getX(), e.getY());
				Log.d(THIS, "Drag");
				return false;
			case MotionEvent.ACTION_UP:
				mode = Mode.None;
				return false;
			case MotionEvent.ACTION_POINTER_DOWN:
				mode = Mode.Zoom;
				touchFilter.redirect = this;
				Log.d(THIS, "Pager blocked");
				Log.d(THIS, "Zoom");
				mBase.set(mCurrent);
				dBegin = distance(e);
				pBegin = midPoint(e);
				return false;
			case MotionEvent.ACTION_POINTER_UP:
				mode = Mode.Drag;
				mBase.set(mCurrent);
				baseScale = mBase.mapRadius(1);
				pBegin = new PointF(e.getX(), e.getY());
				if (baseScale - 0.01 <= 1) {
					touchFilter.redirect = null;
					Log.d(THIS, "Pager allowed");
				}
				return false;
			case MotionEvent.ACTION_MOVE:
				if (mode == Mode.Drag && baseScale <= 1)
					return false;
				
				mCurrent.set(mBase);
				
				PointF d;
				
				if (mode == Mode.Zoom) {
					float s = distance(e) / dBegin;
					if (baseScale * s < 1) {
						Log.d(THIS, "blocked scale down: "+ s);
						s = 1 / baseScale;
						
					}
					mCurrent.postScale(s, s, pBegin.x, pBegin.y);
					
					d = midPoint(e);
				} else {
					d = new PointF(e.getX(), e.getY());
				}
				
				mCurrent.postTranslate(d.x - pBegin.x, d.y - pBegin.y);
				
				break;
			default:
				Log.w(THIS, "Unhandled Action: ");
				dumpEvent(e);
				return false;
			}
			
			
			
			if (mode == Mode.Drag || mode == Mode.Zoom) {
				if (img.getScaleType() != ScaleType.MATRIX)
					img.setScaleType(ScaleType.MATRIX);
				img.setImageMatrix(mCurrent);
				return true;
			} else
				return false;
		}
		
		private float distance(MotionEvent e) {
			float a = e.getX(0) - e.getX(1);
			float b = e.getY(0) - e.getY(1);
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
	
	private static class TouchFilter implements OnTouchListener {
		
		public OnTouchListener redirect = null;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (redirect != null) {
				return redirect.onTouch(v, event);
			} else {
				return false;
			}
		}
		
	}
}
