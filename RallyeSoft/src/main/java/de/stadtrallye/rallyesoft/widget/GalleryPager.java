/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

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
