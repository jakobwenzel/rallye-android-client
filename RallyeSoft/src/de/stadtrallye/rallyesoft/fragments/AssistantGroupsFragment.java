package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.GroupAdapter;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13
 */
public class AssistantGroupsFragment extends SherlockFragment implements IModel.IModelListener {

	private IConnectionAssistant assistant;
	private ListView list;
	private GroupAdapter groupAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_groups, container, false);
		list = (ListView) v.findViewById(R.id.group_list);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			assistant = (IConnectionAssistant) getActivity();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IConnectionAssistant");
		}

		IModel model = assistant.getModel();
		groupAdapter = new GroupAdapter(getActivity(), model.getAvailableGroups(), model);
		list.setAdapter(groupAdapter);

		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				assistant.setGroup((int) id);
				assistant.next();
			}
		});

		restoreChoice();
	}

	private void restoreChoice() {
		int g = assistant.getGroup();
		if (g > 0) {
			list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			list.setItemChecked(g-1, true);
		}
	}


	@Override
	public void onConnectionStateChange(IModel.ConnectionState newState) {

	}

	@Override
	public void onConnectionFailed(Exception e, IModel.ConnectionState fallbackState) {
		Toast.makeText(getActivity(), R.string.invalid_server, Toast.LENGTH_SHORT).show();
		assistant.back();
	}

	@Override
	public void onServerConfigChange() {

	}

	@Override
	public void onServerInfoChange(ServerInfo info) {

	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {
		groupAdapter.changeGroups(groups);

		restoreChoice();
	}
}
