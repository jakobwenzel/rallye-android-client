package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13.
 */
public class AssistantAuthFragment extends SherlockFragment implements View.OnClickListener {


	private IConnectionAssistant assistant;
	private EditText name;
	private EditText pass;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_auth, container, false);
		name = (EditText) v.findViewById(R.id.name);
		pass = (EditText) v.findViewById(R.id.pass);
		Button next = (Button) v.findViewById(R.id.next);

		next.setOnClickListener(this);

		pass.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
	public void onStart() {
		super.onStart();

		String s = assistant.getPass();
		if (s != null) {
			pass.setText(s);
		}
	}

	@Override
	public void onClick(View v) {
		String n = name.getText().toString(), p = pass.getText().toString();

		if (n == null || n.length() < 3) {
			Toast.makeText(getActivity(), R.string.invalid_username, Toast.LENGTH_SHORT).show();
		} else if (pass == null || pass.length() <= 3){
			Toast.makeText(getActivity(), R.string.invalid_password, Toast.LENGTH_SHORT).show();
		} else {
			assistant.setNameAndPass(n, p);
			assistant.next();
		}
	}
}
