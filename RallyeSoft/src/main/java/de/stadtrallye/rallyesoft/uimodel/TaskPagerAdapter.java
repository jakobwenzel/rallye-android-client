package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.fragments.TaskDetailsFragment;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;

/**
 * Adapter to use a Cursor to page through all Tasks in detail
 *
 */
public class TaskPagerAdapter extends FragmentStatePagerAdapter {

	private final Context context;
	private final ITasksMapControl mapControl;
	private Cursor cursor;
	private String ofStr;

	private CursorConverters.TaskCursorIds c;

	public TaskPagerAdapter(FragmentManager fm, Context context, Cursor cursor, ITasksMapControl mapControl) {
		super(fm);
		this.cursor = cursor;
		this.context = context;
		this.mapControl = mapControl;

		this.ofStr = context.getString(R.string.of);

		c = CursorConverters.TaskCursorIds.read(cursor);
	}

	public void changeCursor(Cursor cursor) {
		Cursor old = this.cursor;
		if (old != null) {
			old.close();
		}
		c = CursorConverters.TaskCursorIds.read(cursor);
		this.cursor = cursor;
		this.notifyDataSetChanged();
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object) {
		super.setPrimaryItem(container, position, object);

		if (cursor != null)
			mapControl.setTask(CursorConverters.getTask(position, cursor, c));
	}

	@Override
	public Fragment getItem(int position) {
		Fragment f = new TaskDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(Std.TASK, CursorConverters.getTask(position, cursor, c));
		f.setArguments(args);
		return f;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return (position+1) +" "+ ofStr +" "+ getCount();
	}

	@Override
	public int getCount() {
		return (cursor != null)? cursor.getCount() : 0;
	}
}
