package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.Task;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;
import de.stadtrallye.rallyesoft.model.db.DatabaseHelper;
import static de.stadtrallye.rallyesoft.model.db.DatabaseHelper.getBoolean;

/**
 *
 */
public class TaskCursorAdapter extends CursorAdapter {

	private final LayoutInflater inflator;
	private final Context context;

	private CursorConverters.TaskCursorIds c;

	private class ViewMem {
		public TextView name;
		public ImageButton locate;
	}

	public TaskCursorAdapter(Context context, Cursor cursor) {
		super(context, cursor, false);

		this.context = context;

		this.inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


		c = CursorConverters.TaskCursorIds.read(cursor);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		View v = inflator.inflate(R.layout.task_item, null);

		ViewMem mem = new ViewMem();
		mem.name = (TextView) v.findViewById(R.id.name);
		mem.locate = (ImageButton) v.findViewById(R.id.locate);

		v.setTag(mem);

		fillView(mem, cursor);

		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewMem mem = (ViewMem) view.getTag();

		fillView(mem, cursor);
	}

	private void fillView(ViewMem mem, final Cursor cursor) {
		String name = cursor.getString(c.name);
		final double lat = cursor.getDouble(c.latitude);
		final double lon = cursor.getDouble(c.longitude);

		mem.name.setText(name);
		mem.locate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(context, "Locating "+ cursor.getInt(c.id), Toast.LENGTH_SHORT).show();
//				Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
//						Uri.parse("http://maps.google.com/maps?daddr="+ lat + ","+ lon));
//				context.startActivity(intent);
			}
		});
	}

	@Override
	public void changeCursor(Cursor cursor) {
		c = CursorConverters.TaskCursorIds.read(cursor);
		super.changeCursor(cursor);
	}
}
