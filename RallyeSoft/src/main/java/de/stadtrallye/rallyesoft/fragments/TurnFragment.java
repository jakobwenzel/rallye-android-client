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

package de.stadtrallye.rallyesoft.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import de.rallye.model.structures.AdditionalPicture;
import de.rallye.model.structures.AdditionalResource;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.structures.Task;
import de.stadtrallye.rallyesoft.util.converters.CursorConverters;
import de.stadtrallye.rallyesoft.widget.AdapterView;
import de.stadtrallye.rallyesoft.widget.GridView;
import de.stadtrallye.rallyesoft.widget.ListView;

public class TurnFragment extends Fragment implements GridView.OnItemClickListener {

	private GridView grid;
	private ListView list;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.log_fragment, container, false);
		grid = (GridView) v.findViewById(R.id.log_list);
		list = (ListView) v.findViewById(R.id.grid_comp);
		grid.setOnItemClickListener(this);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();


		Cursor c = null;//TODO

		Task task = CursorConverters.getTask(20, c, CursorConverters.TaskCursorIds.read(c));

		AdditionalGridAdapter adapter = new AdditionalGridAdapter(task.additionalResources);
		grid.setAdapter(adapter);
		list.setAdapter(new AdditionalGridAdapter(task.additionalResources));
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView parent, View view, int position, long id) {

			}
		});
		list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
				return true;
			}
		});

//		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(context, textViewResourceId, objects);
	}

	@Override
	public void onItemClick(AdapterView parent, View view, int position, long id) {
		Toast.makeText(getActivity(), "Click "+ position, Toast.LENGTH_SHORT).show();
	}

	private class AdditionalGridAdapter extends BaseAdapter {

		private final ImageLoader loader;
		private final List<AdditionalResource> resources;

		public AdditionalGridAdapter(List<AdditionalResource> resources) {
			this.loader = ImageLoader.getInstance();
			this.resources = resources;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public int getCount() {
			return (resources != null)? resources.size() : 0;
		}

		@Override
		public Object getItem(int position) {
			return resources.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			AdditionalPicture pic = (AdditionalPicture) resources.get(position);

			ImageView v = (ImageView) convertView;

			if (v == null) {
				v = new ImageView(getActivity());
//				v.setLayoutParams(new GridLayout.LayoutParams(-1,-1));
				v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			}

//			loader.displayImage(model.getUrlFromImageId(pic.pictureID, PictureSize.Mini), v);//TODO

			return v;
		}
	}
}
