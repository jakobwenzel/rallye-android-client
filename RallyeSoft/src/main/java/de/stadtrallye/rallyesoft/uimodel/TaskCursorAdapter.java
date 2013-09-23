package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;
import de.stadtrallye.rallyesoft.model.structures.Task;

/**
 * ListAdapter for Tasks from Cursor
 */
public class TaskCursorAdapter extends CursorAdapter {

	private final LayoutInflater inflator;
//	private final Context context;

	private int headerPos = -1;

	private CursorConverters.TaskCursorIds c;

	private class ViewMem {
		public TextView name;
//		public ImageButton locate;
		public CheckBox check;
	}

	public TaskCursorAdapter(Context context, Cursor cursor) {
		super(context, cursor, false);

//		this.context = context;

		this.inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		c = CursorConverters.TaskCursorIds.read(cursor);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {

		View v = inflator.inflate(R.layout.task_item, null);

		ViewMem mem = new ViewMem();
		mem.name = (TextView) v.findViewById(R.id.name);
//		mem.locate = (ImageButton) v.findViewById(R.id.locate);
		mem.check = (CheckBox) v.findViewById(R.id.checkBox);

		v.setTag(mem);

		fillTaskView(mem, cursor);

		return v;
	}


	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewMem mem = (ViewMem) view.getTag();

		fillTaskView(mem, cursor);
	}

	private void fillTaskView(ViewMem mem, final Cursor cursor) {
		String name = cursor.getString(c.name);
//		final double lat = cursor.getDouble(c.latitude);
//		final double lon = cursor.getDouble(c.longitude);
		final int submits = cursor.getInt(c.submits);

		mem.name.setText(name);
		switch (submits) {
			case Task.SUBMITS_UNKNOWN:
			case Task.SUBMITS_NONE:
				mem.check.setVisibility(View.INVISIBLE);
				break;
			case Task.SUBMITS_SOME:
				mem.check.setVisibility(View.VISIBLE);
				mem.check.setChecked(false);
				break;
			case Task.SUBMITS_COMPLETE:
				mem.check.setVisibility(View.VISIBLE);
				mem.check.setChecked(true);
				break;
		}
//		mem.locate.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Toast.makeText(context, "Locating "+ cursor.getInt(c.id), Toast.LENGTH_SHORT).show();
//				Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
//						Uri.parse("http://maps.google.com/maps?daddr="+ lat + ","+ lon));
//				context.startActivity(intent);
//			}
//		});
	}

	public Task getTask(int position) {
		return CursorConverters.getTask(position, getCursor(), c);
	}

	@Override
	public void changeCursor(Cursor cursor) {
		c = CursorConverters.TaskCursorIds.read(cursor);
		super.changeCursor(cursor);
	}
}
