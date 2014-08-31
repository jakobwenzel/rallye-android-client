/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ListView extends AdapterView {

	private static final String THIS = ListView.class.getSimpleName();

	public ListView(Context context) {
		this(context, null);
	}

	public ListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

//		TypedArray a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.verticalSpacing});
//
//		int vSpacing = a.getDimensionPixelOffset(1, 0);
//		setVerticalSpacing(vSpacing);
//
//		a.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		//		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int suppliedHeight = MeasureSpec.getSize(heightMeasureSpec);

		int widthSize = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);

		int padLeft = getPaddingLeft();
		int padTop = getPaddingTop();
		int padBottom = getPaddingBottom();
		int padRight = getPaddingRight();

		Log.i(THIS, "spacing: v: " + verticalSpacing);

		int childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize - padLeft - padRight, MeasureSpec.EXACTLY);
		int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

		int requiredHeight = padBottom + padTop;
		View v;
		for (int i=0; i < itemCount; i++) {
			v = adapter.getView(i, null, this);
			addView(v, i);
			v.measure(childWidthSpec, childHeightSpec);
			requiredHeight += v.getMeasuredHeight();
			if (i < itemCount)
				requiredHeight += verticalSpacing;
		}

		Log.i(THIS, "Calculated minimum height: "+ requiredHeight);
		int heightSize;

		switch (heightMode) {
			case View.MeasureSpec.EXACTLY:
				heightSize = suppliedHeight;
				break;
			case View.MeasureSpec.AT_MOST:
				heightSize = Math.min(requiredHeight, suppliedHeight);
				break;
			case View.MeasureSpec.UNSPECIFIED:
			default:
				heightSize = requiredHeight;
				break;
		}
		Log.i(THIS, "HeightSpec: "+ heightMode +", set height: "+ heightSize);

		setMeasuredDimension(widthSize, heightSize);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		Log.i(THIS, "onLayout: "+ left +","+ top +","+ right +","+ bottom);

		int currentLeft = 0, currentTop = 0;
		int currentHeight;

		View v;
		for (int i=0; i < itemCount; i++) {
			v = getChildAt(i);

			currentHeight = v.getMeasuredHeight();
			Log.i(THIS, "placing view "+ i +" at "+ currentLeft +", "+ currentTop +" size: "+ v.getMeasuredWidth() +", "+ currentHeight);

			v.layout(currentLeft, currentTop, currentLeft + v.getMeasuredWidth(), currentTop + currentHeight);

			currentTop += currentHeight;

			if (i < itemCount)
				currentTop += verticalSpacing;
		}

		selectorRect.setEmpty();
	}
}
