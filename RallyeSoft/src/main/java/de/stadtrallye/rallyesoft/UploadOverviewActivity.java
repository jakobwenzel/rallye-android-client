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

package de.stadtrallye.rallyesoft;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Queue;

import de.stadtrallye.rallyesoft.model.pictures.PictureManager;
import de.stadtrallye.rallyesoft.services.UploadService;

/**
 * Created by Ramon on 03.10.2014.
 */
public class UploadOverviewActivity extends FragmentActivity implements ServiceConnection {

	private ListView lst_uploads;
	private UploadService.UploadBinder uploadService;
	private UploadAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = new Intent(this, UploadService.class);
		bindService(intent, this, BIND_AUTO_CREATE);

		setContentView(R.layout.upload_overview_activity);

		lst_uploads = (ListView) findViewById(R.id.upload_overview);
		lst_uploads.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				final PictureManager.Picture item = adapter.getItem(position);
				if (!item.isUploading()) {
					item.discard();
					adapter.notifyDataSetChanged();
					return true;
				} else {
					return false;
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unbindService(this);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		this.uploadService = (UploadService.UploadBinder)service;
		adapter = new UploadAdapter(this, uploadService);
		lst_uploads.setAdapter(adapter);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		uploadService = null;

	}
}

class UploadAdapter extends BaseAdapter implements UploadService.IUploadListener {

	private final UploadService.UploadBinder service;
	private final Context context;
	private final LayoutInflater inflater;
	private final ImageLoader imageLoader;
	private UploadService.UploadStatus uploadStatus;
	private ArrayList<PictureManager.Picture> wifiUploads = new ArrayList<>();
	private ArrayList<PictureManager.Picture> otherUploads = new ArrayList<>();

	private static class ViewMem {

		public ImageView thumb;
		public TextView name;
		public TextView desc;
		public ProgressBar progress;
	}

	public UploadAdapter(Context context, UploadService.UploadBinder service) {
		this.service = service;
		this.context = context;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		service.setListener(this);
		this.imageLoader = ImageLoader.getInstance();
		this.uploadStatus = service.getUploadStatus();

		loadQueue();
	}

	private void loadQueue() {
		wifiUploads.clear();
		otherUploads.clear();

		Queue<PictureManager.Picture> queue = service.getCurrentQueue();
		for (PictureManager.Picture p : queue) {
			if (p.isPreviewUploaded()) {
				wifiUploads.add(p);
			} else {
				otherUploads.add(p);
			}
		}
		notifyDataSetChanged();
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		return (position == otherUploads.size())? 1 : 0;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getCount() {
		return otherUploads.size()+wifiUploads.size()+1;
	}

	@Override
	public PictureManager.Picture getItem(int position) {
		if (position < otherUploads.size()) {
			return otherUploads.get(position);
		} else if (position > otherUploads.size()) {
			return wifiUploads.get(position - otherUploads.size() -1);
		} else {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		PictureManager.Picture p = getItem(position);
		return (p != null)? p.pictureID : -1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		switch (getItemViewType(position)) {
			case 0: // Picture
				View v = convertView;
				ViewMem mem;
				if (v == null) {
					v = inflater.inflate(R.layout.upload_item, parent, false);
					mem = new ViewMem();
					mem.thumb = (ImageView) v.findViewById(R.id.upload_thumb);
					mem.name = (TextView) v.findViewById(R.id.upload_name);
					mem.desc = (TextView) v.findViewById(R.id.upload_desc);
					mem.progress = (ProgressBar) v.findViewById(R.id.upload_progress);
					v.setTag(mem);
				} else {
					mem = (ViewMem) v.getTag();
				}

				PictureManager.Picture p = getItem(position);
				mem.name.setText(context.getString(R.string.picture_no_x, p.pictureID));
				mem.desc.setText(getSourceDescription(p.getSourceHint()));
				imageLoader.displayImage(p.getUri(), mem.thumb);
				if (uploadStatus != null && uploadStatus.picture == p) {//equals
					mem.progress.setMax(uploadStatus.biteCount);
					mem.progress.setProgress(uploadStatus.getBiteProgress());
					mem.progress.setIndeterminate(uploadStatus.getIndeterminate());
					mem.progress.setVisibility(View.VISIBLE);
				} else {
					mem.progress.setVisibility(View.INVISIBLE);
				}

				return v;
			case 1:// Separator between other / wifi
				TextView tv = (convertView == null)? (TextView) inflater.inflate(android.R.layout.preference_category, parent, false) : (TextView) convertView;
				tv.setText(R.string.original_file_uploads);
				return tv;
		}

		return null;
	}

	private String getSourceDescription(PictureManager.SourceHint sourceHint) {
		if (sourceHint == null) {
			return context.getString(R.string.unknown);
		}

		String sourceName = null;
		switch (sourceHint.source) {
			case PictureManager.SOURCE_CHAT:
				sourceName = context.getString(R.string.chat);
				break;
			case PictureManager.SOURCE_SUBMISSION:
				sourceName = context.getString(R.string.submission);
				break;
			default:
				sourceName = context.getString(R.string.unknown);
		}

		return sourceName +" "+ sourceHint.name;
	}

	@Override
	public void onUploadStatusChange() {
		uploadStatus = service.getUploadStatus();
		notifyDataSetChanged();
	}

	@Override
	public void onQueueChange() {
		loadQueue();
	}
}
