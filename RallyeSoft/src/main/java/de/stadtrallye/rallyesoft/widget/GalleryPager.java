package de.stadtrallye.rallyesoft.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class GalleryPager extends ViewPager {
	
	private boolean touchEnabled = true;
	private boolean cancelled = false;

	public GalleryPager(Context context) {
		super(context);
	}
	
	public GalleryPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setInterceptTouch(boolean enabled) {
		touchEnabled = enabled;
		cancelled = enabled;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (touchEnabled) {
			return super.onInterceptTouchEvent(ev);
		} else {
			if (!cancelled) {
				MotionEvent e = MotionEvent.obtain(ev);
				e.setAction(MotionEvent.ACTION_CANCEL);
				super.onInterceptTouchEvent(e);
				cancelled = true;
				e.recycle();
			}
			return false;
		}
	}

}
