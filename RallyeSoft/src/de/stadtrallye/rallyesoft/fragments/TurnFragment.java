package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.IModel.ConnectionState;

//TODO
public class TurnFragment extends SherlockFragment implements IModel.IModelListener {

	private ListView list;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.log_fragment, container, false);
		list = (ListView) v.findViewById(R.id.log_list);
		return v;
	}

	@Override
	public void onStart() {
		super.onStart();

//		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(context, textViewResourceId, objects);
	}

	@Override
	public void onConnectionStateChange(IModel.ConnectionState newState) {

	}

	@Override
	public void onConnectionFailed(Exception e, ConnectionState fallbackState) {

	}

	@Override
	public void onServerConfigChange() {

	}

	@Override
	public void onServerInfoChange(ServerInfo info) {

	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {

	}


}
