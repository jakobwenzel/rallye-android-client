package de.stadtrallye.rallyesoft.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import de.rallye.model.structures.AdditionalPicture;
import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.Group;
import de.rallye.model.structures.PictureSize;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.IModel.ConnectionState;
import de.stadtrallye.rallyesoft.model.converters.CursorConverters;
import de.stadtrallye.rallyesoft.model.structures.Task;
import de.stadtrallye.rallyesoft.widget.AdapterView;
import de.stadtrallye.rallyesoft.widget.GridView;
import de.stadtrallye.rallyesoft.widget.ListView;

import static de.stadtrallye.rallyesoft.model.Model.getModel;

public class TurnFragment extends SherlockFragment implements IModel.IModelListener, GridView.OnItemClickListener {

	private GridView grid;
	private ListView list;
	private IModel model;

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

		model = getModel(getActivity());
		Cursor c = model.getTasks().getTasksCursor();

		Task task = CursorConverters.getTask(20, c, CursorConverters.TaskCursorIds.read(c));

		AdditionalGridAdapter adapter = new AdditionalGridAdapter(task.additionalResources);
		grid.setAdapter(adapter);
		list.setAdapter(new AdditionalGridAdapter(task.additionalResources));

//		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(context, textViewResourceId, objects);
	}

	@Override
	public void onConnectionStateChange(IModel.ConnectionState newState) {

	}

	@Override
	public void onConnectionFailed(Exception e, ConnectionState fallbackState) {

	}

	@Override
	public void onServerInfoChange(ServerInfo info) {

	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {

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

			loader.displayImage(model.getUrlFromImageId(pic.pictureID, PictureSize.Mini), v);

			return v;
		}
	}
}
