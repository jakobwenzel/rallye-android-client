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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GridView extends AdapterView {

	private static final String THIS = GridView.class.getSimpleName();

	private static enum ColumnMode { ForceWidth, ForceNum }

	private int horizontalSpacing;
	private int columnWidth;
	private int numColumns;
	private ColumnMode columnMode;
	private boolean scaleSpacing = false;

	public GridView(Context context) {
		super(context, null);
	}

	public GridView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public GridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.horizontalSpacing,
				android.R.attr.columnWidth, android.R.attr.numColumns});

		int hSpacing = a.getDimensionPixelOffset(0, 0);
		setHorizontalSpacing(hSpacing);

		int columnWidth = a.getDimensionPixelOffset(1, -1);
		if (columnWidth > 0) {
			setColumnWidth(columnWidth);
		}

		int numColumns = a.getInt(2, -1);
		if (numColumns > 0) {
			setNumColumns(numColumns);
		}

		a.recycle();
	}

	public void setHorizontalSpacing(int horizontalSpacing) {
		this.horizontalSpacing = horizontalSpacing;
	}

	public int getHorizontalSpacing() {
		return horizontalSpacing;
	}

	/**
	 * Mutually exclusive with {@link #setNumColumns(int)}
	 * @param columnWidth the width of every column (and height of every row), number of colums will be calculated based on this
	 */
	public void setColumnWidth(int columnWidth) {
		columnMode = ColumnMode.ForceWidth;
		this.columnWidth = columnWidth;
	}

	public int getColumnWidth() {
		return columnWidth;
	}

	/**
	 * Mutually exclusive with {@link #setColumnWidth(int)}
	 * @param numColumns number of columns, column width will be calculated based on this
	 */
	public void setNumColumns(int numColumns) {
		columnMode = ColumnMode.ForceNum;
		this.numColumns = numColumns;
	}

	public int getNumColumns() {
		return numColumns;
	}

	/**
	 *
	 * @param scaleSpacing true = scale columns, false = scale spacing
	 */
	public void setScaleSpacing(boolean scaleSpacing) {
		this.scaleSpacing = scaleSpacing;
	}

	public boolean getScaleSpacing() {
		return scaleSpacing;
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		//		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
//		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int suppliedHeight = View.MeasureSpec.getSize(heightMeasureSpec);

		int widthSize = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);

		int padLeft = getPaddingLeft();
		int padTop = getPaddingTop();
		int padBottom = getPaddingBottom();
		int padRight = getPaddingRight();

		Log.i(THIS, "spacing: h: "+ horizontalSpacing +", v: "+ verticalSpacing);

		int cols, colWidth;

		if (columnMode == ColumnMode.ForceNum) {
			cols = numColumns;
			columnWidth = colWidth = (widthSize - padLeft - padRight - cols * horizontalSpacing) / cols;
			Log.i(THIS, "Force "+ cols +" columns -> width: "+ colWidth);
		} else if (columnMode == ColumnMode.ForceWidth) {
			colWidth = columnWidth;
			numColumns = cols = (widthSize + horizontalSpacing) / (colWidth + horizontalSpacing);
			Log.i(THIS, "Force width: "+ colWidth +" -> columns: "+ cols);

			int usedWith = cols * colWidth + Math.max((cols-1) * horizontalSpacing, 0);
			int surplus = widthSize - usedWith;
			if (scaleSpacing && cols > 1) {
				horizontalSpacing = colWidth + surplus / (cols -1);
				Log.i(THIS, "ScaleSpacing: true => horizontalSpacing: "+ horizontalSpacing);
			} else {
				colWidth = columnWidth = colWidth + surplus / cols;
				Log.i(THIS, "ScaleSpacing: "+ scaleSpacing +" => columnWidth: "+ colWidth);
			}

		} else {
			throw new IllegalStateException("Need setNumColumn or setColumnWidth");
		}

		int rows = (int) Math.ceil(itemCount / (float) cols);
		int requiredHeight = rows * colWidth + padBottom + padTop + ((rows > 0)? (rows-1)* verticalSpacing : 0);
		Log.i(THIS, "Calculated minimum height: "+ requiredHeight +" using "+ rows +" rows");
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

		int childHeightSpec = View.MeasureSpec.makeMeasureSpec(colWidth, View.MeasureSpec.AT_MOST);
		int childWidthSpec = View.MeasureSpec.makeMeasureSpec(colWidth, View.MeasureSpec.AT_MOST);

		View v;
		for (int i=0; i< itemCount; i++) {
			v = adapter.getView(i, null, this);
			addView(v, i);
			v.measure(childWidthSpec, childHeightSpec);
		}

		setMeasuredDimension(widthSize, heightSize);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

		Log.i(THIS, "onLayout: "+ left +","+ top +","+ right +","+ bottom);

		int row = 0, col = 0;

		int currentLeft, currentTop;

		View v;
		for (int i=0; i < itemCount; i++) {
			v = getChildAt(i);

			currentLeft = col * columnWidth + col * horizontalSpacing;
			currentTop = row * columnWidth + row * verticalSpacing;

			Log.i(THIS, "placing view"+ i +" into "+ col +","+ row +" at "+ currentLeft +", "+ currentTop +" size: "+ columnWidth +", "+ columnWidth);

			v.layout(currentLeft, currentTop, currentLeft + columnWidth, currentTop + columnWidth);

			if (++col >= numColumns) {
				col = 0;
				row++;
			}
		}

		selectorRect.setEmpty();
	}
}
