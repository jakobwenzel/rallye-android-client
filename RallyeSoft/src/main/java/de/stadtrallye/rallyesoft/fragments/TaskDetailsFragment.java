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
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import de.rallye.model.structures.AdditionalPicture;
import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.PictureSize;
import de.rallye.model.structures.Submission;
import de.rallye.model.structures.Task;
import de.stadtrallye.rallyesoft.PictureGalleryActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.SubmitNewSolutionActivity;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.pictures.PictureGallery;
import de.stadtrallye.rallyesoft.model.tasks.ITask;
import de.stadtrallye.rallyesoft.model.tasks.ITaskManager;
import de.stadtrallye.rallyesoft.net.PictureIdResolver;
import de.stadtrallye.rallyesoft.threading.Threading;
import de.stadtrallye.rallyesoft.widget.AdapterView;
import de.stadtrallye.rallyesoft.widget.GridView;

/**
 * List of all Tasks that have no location attached to them
 */
public class TaskDetailsFragment extends Fragment implements AdapterView.OnItemClickListener, ITask.ITaskListener {

	private static final String THIS = TaskDetailsFragment.class.getSimpleName();

	//Views
	private TextView name;
	private TextView desc;
	private GridView additionalGrid;
	private Button submit;
	private TextView type;
	private ListView list;

	//Adapters
	private AdditionalGridAdapter gridAdapter;
	private SubmissionListAdapter submissionAdapter;

	//Logic
	private ITaskManager taskManager;
	private ITask task;
	private PictureIdResolver imageResolver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Server server = Server.getCurrentServer();
		taskManager = server.acquireTaskManager(this);
		imageResolver = server.getPictureResolver();

		task = taskManager.getTask(getArguments().getInt(Std.TASK_ID));

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		list = (ListView) inflater.inflate(R.layout.tasks_details, container, false);
		View header = inflater.inflate(R.layout.task_details_header, list, false);
		View footer = inflater.inflate(R.layout.task_details_footer, list, false);

		name = (TextView) header.findViewById(R.id.task_name);
		type = (TextView) header.findViewById(R.id.task_type);
		desc = (TextView) header.findViewById(R.id.task_description);
		additionalGrid = (GridView) header.findViewById(R.id.additional_info);
//		submissionsList = (ListView) v.findViewById(R.id.submissions);

		gridAdapter = new AdditionalGridAdapter();
		additionalGrid.setAdapter(gridAdapter);
		additionalGrid.setOnItemClickListener(this);

		list.addHeaderView(header);
		list.addFooterView(footer);
		submissionAdapter = new SubmissionListAdapter();
		list.setAdapter(submissionAdapter);

		submit = (Button) footer.findViewById(R.id.submit);
		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				submitNewSolution();
			}
		});

		return list;
	}

	private void updateTaskInfo() {
		name.setText(task.getName());
		desc.setText(task.getDescription());
//		type.setText();

		gridAdapter.notifyDataSetChanged();
	}

	@Override
	public void onStart() {
		super.onStart();

		updateTaskInfo();

		task.addListener(this);

		List<Submission> submissions = task.getSubmissions();
		if (submissions != null) {
			submissionAdapter.changeList(submissions);
		} else {
			task.updateSubmissions();
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		task.removeListener(this);
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
		intent.putExtra(Std.SUBMIT_TYPE, task.getSubmitType());
		intent.putExtra(Std.TASK_ID, task.getTaskID());
		startActivityForResult(intent, SubmitNewSolutionActivity.REQUEST_CODE);
	}

	@Override
	public void onItemClick(AdapterView parent, View view, int position, long id) {
		Intent intent = new Intent(getActivity(), PictureGalleryActivity.class);

		final List<AdditionalResource> additionalResources = task.getAdditionalResources();
		String[] pics = new String[additionalResources.size()];
		for (int i=0; i<additionalResources.size(); ++i) {
			pics[i] = ((AdditionalPicture) additionalResources.get(i)).pictureHash;
		}

		intent.putExtra(Std.PICTURE_GALLERY, new PictureGallery(position, pics, imageResolver));
		startActivity(intent);
	}

//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.v(THIS, "got result: " + resultCode);
//		if (resultCode== Activity.RESULT_OK) {
//
//			Log.v(THIS, "need to update submissions");
//		}
//	}

	@Override
	public void onSubmissionsChanged(List<Submission> submissions) {
		submissionAdapter.changeList(submissions);
	}

	@Override
	public void onTaskChange() {
		updateTaskInfo();
	}

	@Override
	public Handler getCallbackHandler() {
		return Threading.getUiExecutor();
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
			return (task.getAdditionalResources() != null)? task.getAdditionalResources().size() : 0;
		}

		@Override
		public Object getItem(int position) {
			return task.getAdditionalResources().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			AdditionalPicture pic = (AdditionalPicture) task.getAdditionalResources().get(position);

			ImageView v = (ImageView) convertView;

			if (v == null) {
				v = new ImageView(getActivity());
//				v.setAdjustViewBounds(true);
//				v.setLayoutParams(new GridLayout.LayoutParams(-1,-1));
				v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			}

			loader.displayImage(imageResolver.resolvePictureID(pic.pictureHash, PictureSize.Mini), v);

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
//
//
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

				String picHash = sub.picSubmission;
				if (picHash==null)
					return v;

				loader.displayImage(imageResolver.resolvePictureID(picHash, PictureSize.Mini), v);
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
}