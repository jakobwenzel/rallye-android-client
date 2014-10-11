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
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.model.chat.IChatManager;
import de.stadtrallye.rallyesoft.model.map.IMapManager;
import de.stadtrallye.rallyesoft.model.tasks.ITaskManager;
import de.stadtrallye.rallyesoft.storage.Storage;

/**
 * Created by Ramon on 29.09.2014.
 */
public class ForceRefreshPreference extends DialogPreference {
	private static final String THIS = ForceRefreshPreference.class.getSimpleName();

	private final IMapManager mapManager;
	private final IChatManager chatManager;
	private final ITaskManager taskManager;
	private final Server currentServer;
	private final int target;

	public ForceRefreshPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ForceRefreshPreference);

		target = a.getInt(R.styleable.ForceRefreshPreference_target, 0);

		a.recycle();

		Storage.aquireStorage(context.getApplicationContext(), this);
		currentServer = Server.getCurrentServer();
		chatManager = currentServer.acquireChatManager(this);
		taskManager = currentServer.acquireTaskManager(this);
		mapManager = currentServer.acquireMapManager(this);

		setDialogTitle(getTitleRes());
		setDialogMessage(R.string.confirm_force_refresh);
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);

	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			switch (target) {
				case 1:
					Log.i(THIS, "Force refreshing mapconfig");
					mapManager.forceRefreshMapConfig();
					break;
				case 2:
					Log.i(THIS, "Force refreshing available chatrooms");
					chatManager.forceRefreshChatrooms();
					break;
				case 3:
					Log.i(THIS, "Force refreshing tasks");
					taskManager.forceRefresh();
					break;
				default:
					Log.e(THIS, "Target unknown");
			}
		}
	}

	@Override
	public void onActivityDestroy() {
		super.onActivityDestroy();

		currentServer.releaseChatManager(this);
		currentServer.releaseTaskManager(this);
		currentServer.releaseMapManager(this);
		Storage.releaseStorage(this);
	}
}
