package de.stadtrallye.rallyesoft.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ListAdapter;

public abstract class AdapterView extends ViewGroup implements GestureDetector.OnGestureListener {

	private static final String THIS = AdapterView.class.getSimpleName();

	public static final int INVALID_POSITION = -1;

	protected ListAdapter adapter;
	protected DataSetObserver dataSetObserver;
	protected boolean dataChanged;
	protected int itemCount;
	protected int oldItemCount;
	protected Drawable selector;

	protected Rect listPadding = new Rect();
	protected Rect selectorRect = new Rect();
	protected int mSelectionLeftPadding = 0;
	protected int mSelectionTopPadding = 0;
	protected int mSelectionRightPadding = 0;
	protected int mSelectionBottomPadding = 0;
	private boolean drawSelectorOnTop;
	protected int verticalSpacing;
	protected int touchPos;

	private GestureDetector gestureDetector;

	/**
	 * Rectangle used for hit testing children
	 */
	private Rect touchFrame;

	/**
	 * The listener that receives notifications when an item is clicked.
	 */
	private OnItemClickListener onItemClickListener;

	/**
	 * The listener that receives notifications when an item is long clicked.
	 */
	private OnItemLongClickListener onItemLongClickListener;


	public AdapterView(Context context) {
		this(context, null);
	}

	public AdapterView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AdapterView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.verticalSpacing, android.R.attr.drawSelectorOnTop, android.R.attr.listSelector});

		int vSpacing = a.getDimensionPixelOffset(0, 0);
		setVerticalSpacing(vSpacing);

		boolean selectorOnTop = a.getBoolean(1, true);
		setDrawSelectorOnTop(selectorOnTop);

		Drawable d = a.getDrawable(2);
		if (d != null) {
			setSelector(d);
		} else {
			useDefaultSelector();
		}

		a.recycle();

		gestureDetector = new GestureDetector(getContext(), this);
//		gestureDetector.setIsLongpressEnabled(false);
	}

	public void setVerticalSpacing(int verticalSpacing) {
		this.verticalSpacing = verticalSpacing;
	}

	public int getVerticalSpacing() {
		return verticalSpacing;
	}

	public void setDrawSelectorOnTop(boolean drawSelectorOnTop) {
		this.drawSelectorOnTop = drawSelectorOnTop;
	}

	public boolean isDrawSelectorOnTop() {
		return drawSelectorOnTop;
	}

	private class AdapterDataSetObserver extends DataSetObserver {

		private Parcelable instanceState = null;

		@Override
		public void onChanged() {
			dataChanged = true;
			oldItemCount = itemCount;
			itemCount = adapter.getCount();

			requestLayout();
		}

		@Override
		public void onInvalidated() {
			dataChanged = true;

			if (adapter.hasStableIds()) {
				// Remember the current state for the case where our hosting activity is being
				// stopped and later restarted
				instanceState = onSaveInstanceState();
			}

			// Data is invalid so we should reset our state
			oldItemCount = itemCount;
			itemCount = 0;

			requestLayout();
		}

		public void clearSavedState() {
			instanceState = null;
		}

	}

	public void setAdapter(ListAdapter adapter) {
		if (this.adapter != null && this.dataSetObserver != null) {
			this.adapter.unregisterDataSetObserver(dataSetObserver);
		}

		//reset
//		resetList();
//		mRecycler.clear();
//		mAdapter = adapter;
//
//		mOldSelectedPosition = INVALID_POSITION;
//		mOldSelectedRowId = INVALID_ROW_ID;

		this.adapter = adapter;

		if (adapter != null) {
			if (!adapter.hasStableIds())
				throw new IllegalArgumentException("Adapter must have stable ids");

			oldItemCount = itemCount;
			itemCount = adapter.getCount();
			dataChanged = true;

			dataSetObserver = new AdapterDataSetObserver();
			adapter.registerDataSetObserver(dataSetObserver);

			Log.i(THIS, "New Adapter set: " + itemCount + " entries");

//			int position;
//			if (mStackFromBottom) {
//				position = lookForSelectablePosition(mItemCount - 1, false);
//			} else {
//				position = lookForSelectablePosition(0, true);
//			}
//			setSelectedPositionInt(position);
//			setNextSelectedPositionInt(position);
//			checkSelectionChanged();
		}

		requestLayout();
	}

	public ListAdapter getAdapter() {
		return adapter;
	}

	/**
	 * Set a Drawable that should be used to highlight the currently selected item.
	 * @param resID A Drawable resource to use as the selection highlight.
	 */
	public void setSelector(int resID) {
		setSelector(getResources().getDrawable(resID));
	}

	public void setSelector(Drawable sel) {
		if (selector != null) {
			selector.setCallback(null);
			unscheduleDrawable(selector);
		}
		selector = sel;
		Rect padding = new Rect();
		sel.getPadding(padding);
		mSelectionLeftPadding = padding.left;
		mSelectionTopPadding = padding.top;
		mSelectionRightPadding = padding.right;
		mSelectionBottomPadding = padding.bottom;
		sel.setCallback(this);
		updateSelectorState();
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		updateSelectorState();
	}

	private void updateSelectorState() {
		selector.setState(getDrawableState());
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		if (!drawSelectorOnTop) {
			drawSelector(canvas);
		}

		super.dispatchDraw(canvas);

		if (drawSelectorOnTop) {
			drawSelector(canvas);
		}
	}

	private void drawSelector(Canvas canvas) {
		if (!selectorRect.isEmpty()) {
			selector.setBounds(selectorRect);
			selector.draw(canvas);
		}
	}

	@Override
	protected void dispatchSetPressed(boolean pressed) {

	}

	/**
	 * Returns the selector {@link android.graphics.drawable.Drawable} that is used to draw the
	 * selection in the list.
	 *
	 * @return the drawable used to display the selector
	 */
	public Drawable getSelector() {
		return selector;
	}

	private void useDefaultSelector() {
		setSelector(getResources().getDrawable(android.R.drawable.list_selector_background));
	}

	protected void positionSelector(int position, View sel) {

		selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());
//		if (sel instanceof AbsListView.SelectionBoundsAdjuster) {
//			((AbsListView.SelectionBoundsAdjuster)sel).adjustListItemSelectionBounds(selectorRect);
//		}
		positionSelector(selectorRect.left, selectorRect.top, selectorRect.right,
				selectorRect.bottom);

//		final boolean isChildViewEnabled = mIsChildViewEnabled;
//		if (sel.isEnabled() != isChildViewEnabled) {
//			mIsChildViewEnabled = !isChildViewEnabled;
//			if (getSelectedItemPosition() != INVALID_POSITION) {
//				refreshDrawableState();
//			}
//		}
	}

	private void positionSelector(int l, int t, int r, int b) {
		selectorRect.set(l - mSelectionLeftPadding, t - mSelectionTopPadding, r
				+ mSelectionRightPadding, b + mSelectionBottomPadding);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (selector == null)
			useDefaultSelector();

		removeAllViews();

		itemCount = (adapter == null) ? 0 : adapter.getCount();
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (!isEnabled()) {
			// A disabled view that is clickable still consumes the touch
			// events, it just doesn't respond to them.
			return isClickable() || isLongClickable();
		}

//		if (!isAttached) {
//			// Something isn't right.
//			// Since we rely on being attached to get data set change notifications,
//			// don't risk doing anything where we might try to resync and find things
//			// in a bogus state.
//			return false;
//		}

		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		touchPos = pointToPosition(x, y);

		gestureDetector.onTouchEvent(ev);

		switch (ev.getAction()) {
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				cancelSelector();
				break;
		}

		return true;
	}

	/**
	 * Maps a point to a position in the list.
	 *
	 * @param x X in local coordinate
	 * @param y Y in local coordinate
	 * @return The position of the item which contains the specified point, or
	 *         {@link #INVALID_POSITION} if the point does not intersect an item.
	 */
	private int pointToPosition(int x, int y) {
		if (touchFrame == null) {
			touchFrame = new Rect();
		}

		final int count = getChildCount();
		for (int i = count - 1; i >= 0; i--) {
			final View child = getChildAt(i);
			if (child.getVisibility() == View.VISIBLE) {
				child.getHitRect(touchFrame);
				if (touchFrame.contains(x, y)) {
					return i;
				}
			}
		}
		return INVALID_POSITION;
	}

	/**
	 * Interface definition for a callback to be invoked when an item in this
	 * AdapterView has been clicked.
	 */
	public interface OnItemClickListener {

		/**
		 * Callback method to be invoked when an item in this AdapterView has
		 * been clicked.
		 * <p>
		 * Implementers can call getItemAtPosition(position) if they need
		 * to access the data associated with the selected item.
		 *
		 * @param parent The AdapterView where the click happened.
		 * @param view The view within the AdapterView that was clicked (this
		 *            will be a view provided by the adapter)
		 * @param position The position of the view in the adapter.
		 * @param id The row id of the item that was clicked.
		 */
		void onItemClick(AdapterView parent, View view, int position, long id);
	}

	/**
	 * Register a callback to be invoked when an item in this AdapterView has
	 * been clicked.
	 *
	 * @param listener The callback that will be invoked.
	 */
	public void setOnItemClickListener(OnItemClickListener listener) {
		onItemClickListener = listener;
	}

	/**
	 * @return The callback to be invoked with an item in this AdapterView has
	 *         been clicked, or null id no callback has been set.
	 */
	public final OnItemClickListener getOnItemClickListener() {
		return onItemClickListener;
	}

	/**
	 * Call the OnItemClickListener, if it is defined.
	 *
	 * @param view The view within the AdapterView that was clicked.
	 * @param position The position of the view in the adapter.
	 * @param id The row id of the item that was clicked.
	 * @return True if there was an assigned OnItemClickListener that was
	 *         called, false otherwise is returned.
	 */
	private boolean performItemClick(View view, int position, long id) {
		if (onItemClickListener != null) {
			playSoundEffect(SoundEffectConstants.CLICK);
			if (view != null) {
				view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
			}
			onItemClickListener.onItemClick(this, view, position, id);
			return true;
		}

		return false;
	}

	/**
	 * Interface definition for a callback to be invoked when an item in this
	 * view has been clicked and held.
	 */
	public interface OnItemLongClickListener {
		/**
		 * Callback method to be invoked when an item in this view has been
		 * clicked and held.
		 *
		 * Implementers can call getItemAtPosition(position) if they need to access
		 * the data associated with the selected item.
		 *
		 * @param parent The AbsListView where the click happened
		 * @param view The view within the AbsListView that was clicked
		 * @param position The position of the view in the list
		 * @param id The row id of the item that was clicked
		 *
		 * @return true if the callback consumed the long click, false otherwise
		 */
		boolean onItemLongClick(AdapterView parent, View view, int position, long id);
	}

	/**
	 * Register a callback to be invoked when an item in this AdapterView has
	 * been clicked and held
	 *
	 * @param listener The callback that will run
	 */
	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		if (!isLongClickable()) {
			setLongClickable(true);
		}
		onItemLongClickListener = listener;
	}

	/**
	 * @return The callback to be invoked with an item in this AdapterView has
	 *         been clicked and held, or null id no callback as been set.
	 */
	public final OnItemLongClickListener getOnItemLongClickListener() {
		return onItemLongClickListener;
	}

	private boolean performLongPress(final View child, final int longPressPosition, final long longPressId) {
		// CHOICE_MODE_MULTIPLE_MODAL takes over long press.
//		if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
//			if (mChoiceActionMode == null &&
//					(mChoiceActionMode = startActionMode(mMultiChoiceModeCallback)) != null) {
//				setItemChecked(longPressPosition, true);
//				performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
//			}
//			return true;
//		}

		boolean handled = false;
		if (onItemLongClickListener != null) {
			handled = onItemLongClickListener.onItemLongClick(this, child,
					longPressPosition, longPressId);
		}
		if (!handled) {
//			mContextMenuInfo = createContextMenuInfo(child, longPressPosition, longPressId);
			handled = super.showContextMenuForChild(this);
		}
		if (handled) {
			performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
		}
		return handled;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		Log.d(THIS, "down "+ touchPos);
		View child = getChildAt(touchPos);
		if (child != null) {
			positionSelector(touchPos, child);
			child.setPressed(true);
		}
		setPressed(true);

		final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
		final boolean longClickable = isLongClickable();

		if (selector != null) {
			Drawable d = selector.getCurrent();
			if (d != null && d instanceof TransitionDrawable) {
				if (longClickable) {
					((TransitionDrawable) d).startTransition(longPressTimeout);
				} else {
					((TransitionDrawable) d).resetTransition();
				}
			}
//			refreshDrawableState();
		}
//		invalidate();
		return true;
	}

	@Override
	public void onShowPress(MotionEvent ev) {
		Log.d(THIS, "pressed "+ touchPos);

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		Log.d(THIS, "clicked "+ touchPos);
		cancelSelector();

		performItemClick(getChildAt(touchPos), touchPos, adapter.getItemId(touchPos));

		return true;
	}

	private void cancelSelector() {
		View child = getChildAt(touchPos);
		if (child != null) {
			child.setPressed(false);
		}
		setPressed(false);
//		selectorRect.setEmpty();
//		if (selector != null) {
//			Drawable d = selector.getCurrent();
//			if (d != null && d instanceof TransitionDrawable) {
//				((TransitionDrawable) d).resetTransition();
//			}
////			refreshDrawableState();
//		}
//		invalidate();
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		Log.d(THIS, "scroll");
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		Log.d(THIS, "long pressed "+ touchPos);
		cancelSelector();

		performLongPress(getChildAt(touchPos), touchPos, adapter.getItemId(touchPos));
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		Log.d(THIS, "fling");
		return false;
	}
}
