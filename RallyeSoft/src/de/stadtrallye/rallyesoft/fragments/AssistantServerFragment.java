package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import java.net.MalformedURLException;
import java.net.URL;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13.
 */
public class AssistantServerFragment extends SherlockFragment implements View.OnClickListener {

	private IConnectionAssistant assistant;

	private EditText server;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_server, container, false);
		server = (EditText) v.findViewById(R.id.server);
		Button next = (Button) v.findViewById(R.id.next);
		next.setOnClickListener(this);

		server.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_NEXT) {
					onClick(v);
					handled = true;
				}
				return handled;
			}
		});
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
	}

	@Override
	public void onClick(View v) {
		String s = server.getText().toString();

		if (!s.endsWith("/"))
			s = s+"/";

		try {
			new URL(s);

			assistant.setServer(s);
			assistant.next();
		} catch (MalformedURLException e) {
			Toast.makeText(getActivity(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
		}
	}
}
