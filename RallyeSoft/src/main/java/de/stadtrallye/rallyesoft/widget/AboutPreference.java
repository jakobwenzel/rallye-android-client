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

package de.stadtrallye.rallyesoft.widget;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import de.stadtrallye.rallyesoft.R;

/**
 * Created by Ramon on 30.09.2014.
 */
public class AboutPreference extends DialogPreference {

	public AboutPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		setDialogTitle(R.string.about);
		setDialogIcon(R.drawable.ic_launcher);
		setPositiveButtonText(android.R.string.ok);
		setDialogLayoutResource(R.layout.about_fragment);

	}

	@Override
	protected View onCreateDialogView() {
		View v = super.onCreateDialogView();

		TextView tvGitHub = (TextView) v.findViewById(R.id.about_github);
		TextView tvLibs = (TextView) v.findViewById(R.id.about_libs);
		tvGitHub.setMovementMethod(LinkMovementMethod.getInstance());
		tvLibs.setMovementMethod(LinkMovementMethod.getInstance());

		return v;
	}
}
