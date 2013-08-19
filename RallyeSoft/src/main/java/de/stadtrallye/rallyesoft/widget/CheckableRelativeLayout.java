package de.stadtrallye.rallyesoft.widget;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.RelativeLayout;

/**
 * Created by Ramon on 20.06.13.
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

	private boolean checked = false;


	public CheckableRelativeLayout(Context context) {
		super(context);
	}

	public CheckableRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckableRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setChecked(boolean checked) {
		this.checked = checked;

		if (checked) {
			this.setBackgroundResource(R.color.holo_blue_light);
		} else {
			this.setBackgroundResource(0);
		}
	}

	@Override
	public boolean isChecked() {
		return checked;
	}

	@Override
	public void toggle() {
		setChecked(!checked);
	}
}
