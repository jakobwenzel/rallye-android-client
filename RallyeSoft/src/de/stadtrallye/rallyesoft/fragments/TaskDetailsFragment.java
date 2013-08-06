package de.stadtrallye.rallyesoft.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import de.rallye.model.structures.Task;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.TaskCursorAdapter;

/**
 * List of all Tasks that have no location attached to them
 */
public class TaskDetailsFragment extends SherlockFragment {

	private IModel model;
	private ITasks tasks;
	private TextView name;
	private TextView desc;
	private Task task;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tasks_details, container, false);

		name = (TextView) v.findViewById(R.id.task_name);
		desc = (TextView) v.findViewById(R.id.task_description);

		task = (Task) getArguments().getSerializable(Std.TASK);
		name.setText(task.name);
		desc.setText(task.description);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			model = ((IModelActivity) getActivity()).getModel();
			tasks = model.getTasks();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity");
		}
	}

	@Override
	public void onStart() {
		super.onStart();

	}

	@Override
	public void onStop() {
		super.onStop();

	}
}