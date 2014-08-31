/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * 3. Page of ConnectionAssistant
 * Asks for Username and Group Password
 */
public class AssistantAuthFragment extends Fragment {


	private IConnectionAssistant assistant;
	private EditText name;
	private EditText pass;
	private Button next;

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
		next = (Button) v.findViewById(R.id.next);

		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				View focus = getActivity().getCurrentFocus();
				if (focus != null) {
					InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				}
				onNext();
			}
		});

		pass.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					onNext();
				}
				return false;
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

	private void onNext() {
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
