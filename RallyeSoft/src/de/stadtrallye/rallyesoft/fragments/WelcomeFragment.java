package de.stadtrallye.rallyesoft.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragment;

import de.stadtrallye.rallyesoft.ConnectionAssistant;
import de.stadtrallye.rallyesoft.R;

/**
 * Created by Ramon on 22.06.13.
 */
public class WelcomeFragment extends SherlockFragment {


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.welcome, container, false);

		Button b = (Button) v.findViewById(R.id.connect);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), ConnectionAssistant.class);
				startActivityForResult(intent, ConnectionAssistant.REQUEST_CODE);
			}
		});

		return v;
	}
}
