package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModelListener;

public class TurnFragment extends SherlockFragment implements IModelListener {
	
	private ListView list;

	@Override
	public void onConnectionStatusChange(boolean newStatus) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.log_fragment, container, false);
		
		list = (ListView)v.findViewById(R.id.log_list);
		
		return v;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
//		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(context, textViewResourceId, objects);
	}
	
	
}
