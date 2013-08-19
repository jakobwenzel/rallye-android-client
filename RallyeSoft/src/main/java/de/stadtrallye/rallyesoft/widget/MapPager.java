package de.stadtrallye.rallyesoft.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * ViewPager that will ignore TouchEvents above a certain View
 */
@Deprecated
public class MapPager extends ViewPager {

	private View except;

	public MapPager(Context context) {
		super(context);
	}

	public MapPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		float x = ev.getX(), y = ev.getY();
		if (except == null)
			return super.onInterceptTouchEvent(ev);

		int[] coords = new int[4];
		except.getLocationOnScreen(coords);
		if  (coords[0] + except.getWidth() > x &&		// right edge
				coords[1] + except.getHeight() > y &&	// bottom edge
				coords[0] < x &&						// left edge
				coords[1] < y) {						// top edge
			return false;
		} else
			return super.onInterceptTouchEvent(ev);
	}

	public void setExcept(View except) {
		this.except = except;
	}
}
