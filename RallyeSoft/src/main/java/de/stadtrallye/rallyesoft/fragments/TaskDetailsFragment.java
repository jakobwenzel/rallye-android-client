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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.rallye.model.structures.AdditionalPicture;
import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.PictureSize;
import de.rallye.model.structures.Submission;
import de.rallye.model.structures.Task;
import de.stadtrallye.rallyesoft.PictureGalleryActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.SubmitNewSolutionActivity;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.model.PictureGallery;
import de.stadtrallye.rallyesoft.net.PictureIdResolver;
import de.stadtrallye.rallyesoft.widget.AdapterView;
import de.stadtrallye.rallyesoft.widget.GridView;
import de.stadtrallye.rallyesoft.widget.ListView;

import static de.stadtrallye.rallyesoft.model.Model.getModel;

/**
 * List of all Tasks that have no location attached to them
 */
public class TaskDetailsFragment extends Fragment implements AdapterView.OnItemClickListener, ITasks.ITasksListener {

	private static final String THIS = TaskDetailsFragment.class.getSimpleName();

	private TextView name;
	private TextView desc;
	private Task task;
	private GridView additionalGrid;
	private ListView submissionsList;
	private AdditionalGridAdapter gridAdapter;
	private IModel model;
	private SubmissionListAdapter submissionAdapter;
	private Button submit;
	private TextView type;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		task = (Task) getArguments().getSerializable(Std.TASK);//TODO: only pipe taskID? (on the other hand: if whole ids are changed, no use + the adapter will reload anything anyway)
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tasks_details, container, false);

		name = (TextView) v.findViewById(R.id.task_name);
		type = (TextView) v.findViewById(R.id.task_type);
		desc = (TextView) v.findViewById(R.id.task_description);
		additionalGrid = (GridView) v.findViewById(R.id.additional_info);
		submissionsList = (ListView) v.findViewById(R.id.submissions);

		name.setText(task.name);
		desc.setText(task.description);
//		type.setText();

		gridAdapter = new AdditionalGridAdapter();
		additionalGrid.setAdapter(gridAdapter);
		additionalGrid.setOnItemClickListener(this);

		submissionAdapter = new SubmissionListAdapter();
		submissionsList.setAdapter(submissionAdapter);

		submit = (Button) v.findViewById(R.id.submit);
		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				submitNewSolution();
			}
		});

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();

		model = getModel(getActivity());
		ITasks tasks = model.getTasks();
		Map<Integer, List<Submission>> all = tasks.getSubmissions();
		if (all != null) {
			List<Submission> submissions = all.get(task.taskID);
			if (submissions != null)
				submissionAdapter.changeList(submissions);
		}

		tasks.addListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		model.getTasks().removeListener(this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem submitItem = menu.add(Menu.NONE, R.id.submit_menu, Menu.NONE, R.string.submit_new_solution);
		submitItem.setIcon(R.drawable.ic_send_now_light);
		submitItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.submit_menu:
				submitNewSolution();
				return true;
			default:
				return false;
		}
	}

	private void submitNewSolution() {
		Intent intent = new Intent(getActivity(), SubmitNewSolutionActivity.class);
		intent.putExtra(Std.SUBMIT_TYPE, task.submitType);
		intent.putExtra(Std.TASK_ID, task.taskID);
		startActivityForResult(intent, SubmitNewSolutionActivity.REQUEST_CODE);
	}

	@Override
	public void onItemClick(AdapterView parent, View view, int position, long id) {
		Intent intent = new Intent(getActivity(), PictureGalleryActivity.class);

		List<Integer> pics = new ArrayList<Integer>();
		for (AdditionalResource add: task.additionalResources)
			pics.add(((AdditionalPicture)add).pictureID);

		intent.putExtra(Std.PICTURE_GALLERY, new AdditionalGallery(position, pics, model.getPictureIdResolver()));
		startActivity(intent);
	}

	@Override
	public void taskUpdate() {

	}



	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(THIS, "got result: " + resultCode);
		if (resultCode== Activity.RESULT_OK) {

			Log.v(THIS, "need to update submissions");
		}
	}

	@Override
	public void submissionsUpdate(Map<Integer, List<Submission>> submissions) {
		submissionAdapter.changeList(submissions.get(task.taskID));
	}

	private class AdditionalGridAdapter extends BaseAdapter {

		private final ImageLoader loader;

		public AdditionalGridAdapter() {
			this.loader = ImageLoader.getInstance();
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public int getCount() {
			return (task.additionalResources != null)? task.additionalResources.size() : 0;
		}

		@Override
		public Object getItem(int position) {
			return task.additionalResources.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			AdditionalPicture pic = (AdditionalPicture) task.additionalResources.get(position);

			ImageView v = (ImageView) convertView;

			if (v == null) {
				v = new ImageView(getActivity());
//				v.setAdjustViewBounds(true);
//				v.setLayoutParams(new GridLayout.LayoutParams(-1,-1));
				v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			}

			loader.displayImage(model.getUrlFromImageId(pic.pictureID, PictureSize.Mini), v);

			return v;
		}
	}

	private class SubmissionListAdapter extends BaseAdapter {

		private final LayoutInflater inflator;
		private List<Submission> submissions;
		private final ImageLoader loader;

		/*private class ViewMem {
			public ImageView img;
		}*/

		public SubmissionListAdapter() {
			this.loader = ImageLoader.getInstance();
			this.inflator = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		public void changeList(List<Submission> submissions) {
			Log.d(THIS, "New List backing SubmissionAdapter, "+ ((submissions!= null)? submissions.size() : 0) +" items");
			this.submissions = submissions;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return (submissions != null)? submissions.size() : 0;
		}

		@Override
		public Object getItem(int position) {
			return submissions.get(position);
		}

		@Override
		public long getItemId(int position) {
			return submissions.get(position).submissionID;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Submission sub = submissions.get(position);

//			View v;
//			ViewMem mem;


//			if (convertView == null) {
//				v = inflator.inflate(R.layout.submission_item, null);
//				mem = new ViewMem();
//				mem.img = (ImageView) v.findViewById(R.id.image);
//				v.setTag(mem);
//			} else {
//				v = convertView;
//				mem = (ViewMem) v.getTag();
//			}

			//Is it image?
			if ((sub.submitType & Task.TYPE_PICTURE) >0) {
				ImageView v = (ImageView) convertView;

				if (v == null) {
					v = new ImageView(getActivity());
//				v.setAdjustViewBounds(true);
//				v.setLayoutParams(new GridLayout.LayoutParams(-1,-1));
					v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				}

				Integer picID = sub.intSubmission;
				if (picID==null)
					return v;

				loader.displayImage(model.getUrlFromImageId(picID, PictureSize.Mini), v);
				return v;
			} else { //We currently only support text and num,bers
				TextView v = (TextView) convertView;

				if (v == null) {
					v = new TextView(getActivity());
				}
				if (sub.intSubmission!=null)
					v.setText(sub.intSubmission.toString());
				else
					v.setText(sub.textSubmission);
				return v;
			}
		}
	}

	private static class AdditionalGallery extends PictureGallery {

		private final PictureIdResolver resolver;
		private final List<Integer> pictures;
		private final int initialPos;

		public AdditionalGallery(int initialPos, List<Integer> pictures, PictureIdResolver resolver) {
			this.pictures = pictures;
			this.initialPos = initialPos;
			this.resolver = resolver;
		}

		@Override
		public int getInitialPosition() {
			return initialPos;
		}

		@Override
		public int getCount() {
			return pictures.size();
		}

		@Override
		public String getPictureUrl(int pos) {
			return resolver.resolvePictureID(pictures.get(pos), this.size);
		}
	}
}