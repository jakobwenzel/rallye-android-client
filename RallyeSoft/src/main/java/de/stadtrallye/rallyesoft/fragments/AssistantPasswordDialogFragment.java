/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import de.stadtrallye.rallyesoft.R;

/**
 * Created by Ramon on 29.09.2014.
 */
public class AssistantPasswordDialogFragment extends DialogFragment {

	public static final String TAG = AssistantPasswordDialogFragment.class.getCanonicalName();

	private EditText edit_password;
	private IPasswordRetry callback;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		callback = (IPasswordRetry) getParentFragment();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();

		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.password_retry)
				.setView(inflateCustomView(inflater, null, savedInstanceState))
				.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						callback.retryWithPassword(edit_password.getText().toString());
					}
				})
				.create();
	}

	private View inflateCustomView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_password_dialog, container, false);

		edit_password = (EditText) v.findViewById(R.id.password);

		return v;
	}

	public interface IPasswordRetry {
		void retryWithPassword(String password);
	}
}
