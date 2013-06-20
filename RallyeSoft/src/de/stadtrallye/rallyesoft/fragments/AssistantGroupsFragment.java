package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.List;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.structures.Group;
import de.stadtrallye.rallyesoft.uimodel.GroupAdapter;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13.
 */
public class AssistantGroupsFragment extends SherlockFragment implements IModel.IAvailableGroupsCallback {


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

		if (groupAdapter == null) {
			assistant.getModel().getAvailableGroups(this, assistant.getServer());
		} else {
			initList();
			restoreChoice();
		}
	}

	private void restoreChoice() {
		int g = assistant.getGroup();
		if (g > 0) {
			list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			list.setItemChecked(g-1, true);
		}
	}

	private void initList() {
		list.setAdapter(groupAdapter);

		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				assistant.setGroup((int) id);
				assistant.next();
			}
		});
	}

	@Override
	public void availableGroups(List<Group> groups) {
		if (groups != null && groups.size() > 0) {

			groupAdapter = new GroupAdapter(getActivity(), groups, assistant.getModel());

			initList();

			restoreChoice();
		} else {
			Toast.makeText(getActivity(), R.string.invalid_server, Toast.LENGTH_SHORT).show();
			assistant.back();
		}
	}
}
