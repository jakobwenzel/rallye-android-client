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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import de.stadtrallye.rallyesoft.ConnectionAssistantActivity;
import de.stadtrallye.rallyesoft.R;

/**
 * Shown on first start, hints at ConnectionAssistant
 */
public class WelcomeFragment extends Fragment {


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.welcome, container, false);

		Button b = (Button) v.findViewById(R.id.connect);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), ConnectionAssistantActivity.class);
//				getTabManager(getActivity()).switchToTab(RallyeTabManager.TAB_WAIT_FOR_MODEL);
				startActivityForResult(intent, ConnectionAssistantActivity.REQUEST_CODE);
			}
		});

		return v;
	}
}
