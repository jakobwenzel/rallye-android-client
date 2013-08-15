package de.stadtrallye.rallyesoft.widget;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;

/**
 * Created by Ramon on 13.08.13.
 */
public class LinearList extends AdapterView<ListAdapter> {

	private static final String THIS = LinearList.class.getSimpleName();

	private DataSetObserver dataSetObserver;

	private ListAdapter adapter;
	private Drawable selector;
	private boolean adapterHasStableIds;

	public static class LayoutParams extends ViewGroup.LayoutParams {
		/**
		 * View type for this view, as returned by
		 * {@link android.widget.Adapter#getItemViewType(int) }
		 */
		@ViewDebug.ExportedProperty(category = "list", mapping = {
				@ViewDebug.IntToString(from = ITEM_VIEW_TYPE_IGNORE, to = "ITEM_VIEW_TYPE_IGNORE"),
				@ViewDebug.IntToString(from = ITEM_VIEW_TYPE_HEADER_OR_FOOTER, to = "ITEM_VIEW_TYPE_HEADER_OR_FOOTER")
		})
		int viewType;

		/**
		 * The ID the view represents
		 */
		long itemId = -1;

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
		}

		public LayoutParams(int w, int h) {
			super(w, h);
		}

		public LayoutParams(int w, int h, int viewType) {
			super(w, h);
			this.viewType = viewType;
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}
	}


	public LinearList(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();

		// getDrawable?


	}

	public LinearList(Context context) {
		super(context);
		init();
	}

	public LinearList(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private void init() {
		setClickable(true);
	}

	@Override
	public ListAdapter getAdapter() {
		return adapter;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {

	}

	public void setSelector(Drawable selector) {
		this.selector = selector;
	}

	private void useDefaultSelector() {
		setSelector(getResources().getDrawable(android.R.drawable.list_selector_background));
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		return super.onSaveInstanceState();
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		super.onRestoreInstanceState(state);
	}

	private void resetList() {

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (selector == null) {
			useDefaultSelector();
		}
//		final Rect listPadding = mListPadding;
//		listPadding.left = mSelectionLeftPadding + mPaddingLeft;
//		listPadding.top = mSelectionTopPadding + mPaddingTop;
//		listPadding.right = mSelectionRightPadding + mPaddingRight;
//		listPadding.bottom = mSelectionBottomPadding + mPaddingBottom;

		Log.i(THIS, MeasureSpec.toString(widthMeasureSpec));
		Log.i(THIS, MeasureSpec.toString(heightMeasureSpec));

		int width = ViewGroup.getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
		int availableWidth = width - getPaddingLeft() - getPaddingRight();

		int childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY);

		int count = getChildCount();
		int height = getPaddingBottom() + getPaddingTop();

		for (int i=0; i <count; i++) {
			View child = getChildAt(i);
			if (child.getVisibility() == View.GONE)
				continue;

			measureChild(child, childWidthSpec, heightMeasureSpec);
			Log.i(THIS, "Measured Child "+ i +" with height "+ height);
			height += child.getMeasuredHeight();
		}

		Log.i(THIS, "Measured Size "+ width +"x"+ height);
		setMeasuredDimension(width, height);
	}

	//	@Override
//	public boolean performItemClick(View view, int position, long id) {
//		boolean handled = false;
//		boolean dispatchItemClick = true;
//
//		if (mChoiceMode != CHOICE_MODE_NONE) {
//			handled = true;
//			boolean checkedStateChanged = false;
//
//			if (mChoiceMode == CHOICE_MODE_MULTIPLE ||
//					(mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode != null)) {
//				boolean checked = !mCheckStates.get(position, false);
//				mCheckStates.put(position, checked);
//				if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
//					if (checked) {
//						mCheckedIdStates.put(mAdapter.getItemId(position), position);
//					} else {
//						mCheckedIdStates.delete(mAdapter.getItemId(position));
//					}
//				}
//				if (checked) {
//					mCheckedItemCount++;
//				} else {
//					mCheckedItemCount--;
//				}
//				if (mChoiceActionMode != null) {
//					mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode,
//							position, id, checked);
//					dispatchItemClick = false;
//				}
//				checkedStateChanged = true;
//			} else if (mChoiceMode == CHOICE_MODE_SINGLE) {
//				boolean checked = !mCheckStates.get(position, false);
//				if (checked) {
//					mCheckStates.clear();
//					mCheckStates.put(position, true);
//					if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
//						mCheckedIdStates.clear();
//						mCheckedIdStates.put(mAdapter.getItemId(position), position);
//					}
//					mCheckedItemCount = 1;
//				} else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
//					mCheckedItemCount = 0;
//				}
//				checkedStateChanged = true;
//			}
//
//			if (checkedStateChanged) {
//				updateOnScreenCheckedViews();
//			}
//		}
//
//		if (dispatchItemClick) {
//			handled |= super.performItemClick(view, position, id);
//		}
//
//		return handled;
//	}

	@Override
	public View getSelectedView() {
		return null;
	}

	@Override
	public void setSelection(int position) {

	}

	View obtainView(int position) {
		View child;

		child = adapter.getView(position, null, this);

		if (child.getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
			child.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
		}

//		if (mCacheColorHint != 0) {
//			child.setDrawingCacheBackgroundColor(mCacheColorHint);
//		}

		if (adapterHasStableIds) {
			final ViewGroup.LayoutParams vlp = child.getLayoutParams();
			LayoutParams lp;
			if (vlp == null) {
				lp = (LayoutParams) generateDefaultLayoutParams();
			} else if (!checkLayoutParams(vlp)) {
				lp = (LayoutParams) generateLayoutParams(vlp);
			} else {
				lp = (LayoutParams) vlp;
			}
			lp.itemId = adapter.getItemId(position);
			child.setLayoutParams(lp);
		}

//		if (AccessibilityManager.getInstance(mContext).isEnabled()) {
//			if (mAccessibilityDelegate == null) {
//				mAccessibilityDelegate = new ListItemAccessibilityDelegate();
//			}
//			if (child.getAccessibilityDelegate() == null) {
//				child.setAccessibilityDelegate(mAccessibilityDelegate);
//			}
//		}

		return child;
	}
}
