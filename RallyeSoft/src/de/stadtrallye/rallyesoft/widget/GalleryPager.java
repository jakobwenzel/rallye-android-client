package de.stadtrallye.rallyesoft.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class GalleryPager extends ViewPager {
	
	public boolean touchEnabled = true;

	public GalleryPager(Context context) {
		super(context);
	}
	
	public GalleryPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return (touchEnabled)? super.onInterceptTouchEvent(ev) : false;
	}

}
