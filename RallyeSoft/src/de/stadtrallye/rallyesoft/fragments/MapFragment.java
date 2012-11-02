package de.stadtrallye.rallyesoft.fragments;

import java.util.concurrent.ExecutionException;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.communications.Pull;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MapFragment extends Fragment {
	
	Pull pull = new Pull();
	String text;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pull.execute(new String[]{});
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.map_fragment, container, false);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		
		try {
			text = pull.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		Log.w("pull2", text);
		
		Toast.makeText(getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
		
		TextView tv = (TextView) (getView().findViewById(R.id.placeholder));
		tv.setText(text);
	}
}
