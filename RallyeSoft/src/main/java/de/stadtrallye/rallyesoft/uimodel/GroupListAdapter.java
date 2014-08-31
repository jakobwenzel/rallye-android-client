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

package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import de.rallye.model.structures.Group;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;

/**
 * Adapter for showing the Groups available for login on a new server
 * Special customization to pre select a group if user revisits this tab, but already made a choice
 */
public class GroupListAdapter extends BaseAdapter {

	private final IModel model;
	private List<Group> groups;
	private final ImageLoader loader;
//	private Context context;
	private final LayoutInflater inflator;

	private class ViewMem {
		public ImageView img;
		public TextView name;
		public TextView desc;
	}


	public GroupListAdapter(Context context, List<Group> groups, IModel model) {
//		this.context = context;
		this.groups = groups;
		this.model = model;

		this.inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		loader = ImageLoader.getInstance();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		final Group g = groups.get(position);

		ViewMem mem;

		if (v == null) {
			v = inflator.inflate(R.layout.group_item, null);

			mem = new ViewMem();

			mem.img = (ImageView) v.findViewById(R.id.group_img);
			mem.name = (TextView) v.findViewById(R.id.name);
			mem.desc = (TextView) v.findViewById(R.id.desc);

			v.setTag(mem);

		} else {
			mem = (ViewMem) v.getTag();
		}

		if (g != null) {
			mem.name.setText(g.name);
			mem.desc.setText(g.description);

			// ImageLoader jar
			// ImageLoader must apparently be called for _EVERY_ entry
			// When called with null or "" as URL, will display empty pciture / default resource
			// Otherwise ImageLoader will not be stable and start swapping images
			loader.displayImage(model.getAvatarURL(g.groupID), mem.img);
		}

		return v;
	}

	@Override
	public int getCount() {
		return groups.size();
	}

	@Override
	public Group getItem(int position) {
		return groups.get(position);
	}

	@Override
	public long getItemId(int position) {
		return groups.get(position).groupID;
	}

	public Integer findPosition(int group) {
		for (int i=0; i<groups.size(); i++) {
			if (groups.get(i).groupID == group)
				return i;
		}
		return null;
	}

	public void changeGroups(List<Group> groups) {
		this.groups = groups;
		notifyDataSetChanged();
	}
}
