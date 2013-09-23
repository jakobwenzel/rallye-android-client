package de.stadtrallye.rallyesoft.fragments;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
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
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.PictureGallery;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.model.PictureIdResolver;
import de.stadtrallye.rallyesoft.widget.GridView;
import de.stadtrallye.rallyesoft.widget.ListView;

import static de.stadtrallye.rallyesoft.model.Model.getModel;

/**
 * List of all Tasks that have no location attached to them
 */
public class TaskDetailsFragment extends SherlockFragment implements AdapterView.OnItemClickListener, ITasks.ITasksListener {

	private static final String THIS = TaskDetailsFragment.class.getSimpleName();

	private TextView name;
	private TextView desc;
	private Task task;
	private GridView additionalGrid;
	private ListView submissionsList;
	private AdditionalGridAdapter gridAdapter;
	private IModel model;
	private SubmissionListAdapter submissionAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		task = (Task) getArguments().getSerializable(Std.TASK);//TODO: only pipe taskID? (on the other hand: if whole ids are changed, no use + the adapter will reload anything anyway)
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tasks_details, container, false);

		name = (TextView) v.findViewById(R.id.task_name);
		desc = (TextView) v.findViewById(R.id.task_description);
		additionalGrid = (GridView) v.findViewById(R.id.additional_info);
		submissionsList = (ListView) v.findViewById(R.id.submissions);

		name.setText(task.name);
		desc.setText(task.description);

		gridAdapter = new AdditionalGridAdapter();
		additionalGrid.setAdapter(gridAdapter);
//		additionalGrid.setOnItemClickListener(this);

		submissionAdapter = new SubmissionListAdapter();
		submissionsList.setAdapter(submissionAdapter);

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
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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

		private class ViewMem {
			public ImageView img;
		}

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

			View v;
			ViewMem mem;

			if (convertView == null) {
				v = inflator.inflate(R.layout.submission_item, null);
				mem = new ViewMem();
				mem.img = (ImageView) v.findViewById(R.id.image);
				v.setTag(mem);
			} else {
				v = convertView;
				mem = (ViewMem) v.getTag();
			}

			if ((sub.submitType & Task.TYPE_PICTURE) != Task.TYPE_PICTURE)//TODO: fork depending on type
				return v;

			int picID = sub.intSubmission;

			loader.displayImage(model.getUrlFromImageId(picID, PictureSize.Mini), mem.img);

			return v;
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