package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.GroupListAdapter;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Page of ConnectionAssistant: choose a group to login to
 * If the Assistant already knows the group, highlight it
 */
public class AssistantGroupsFragment extends SherlockListFragment implements IModel.IModelListener, AdapterView.OnItemClickListener {

	private IConnectionAssistant assistant;
	private GroupListAdapter groupAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			assistant = (IConnectionAssistant) getActivity();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IConnectionAssistant");
		}
	}

	private void restoreChoice(ListView list) {
		Integer pos = groupAdapter.findPosition(assistant.getGroup());

		if (pos != null) {
			list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			list.setItemChecked(pos, true);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		IModel model = assistant.getModel();
		groupAdapter = new GroupListAdapter(getActivity(), model.getAvailableGroups(), model);
		ListView list = getListView();
		setListAdapter(groupAdapter);

		restoreChoice(list);

		list.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		assistant.setGroup((int) id);
		assistant.next();
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
	public void onServerInfoChange(ServerInfo info) {

	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {
		groupAdapter.changeGroups(groups);

		restoreChoice(getListView());
	}
}
