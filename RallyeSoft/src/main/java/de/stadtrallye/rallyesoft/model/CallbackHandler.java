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
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.model;

import android.os.Handler;
import android.os.Looper;

import de.stadtrallye.rallyesoft.model.chat.IChatManager;

/**
 * Created by Ramon on 22.09.2014.
 */
public class CallbackHandler {

	private static Handler uiThread;

	public static void setupHandler() {
		if (uiThread == null)
			uiThread = new Handler(Looper.getMainLooper());
	}

	public static IChatManager.IChatListener getUiThreadChatListener(IChatManager.IChatListener chatListener) {
		setupHandler();
		return new UiThreadChatListener(chatListener);
	}

	private static class UiThreadChatListener implements IChatManager.IChatListener {

		private final IChatManager.IChatListener listener;

		public UiThreadChatListener(IChatManager.IChatListener listener) {
			this.listener = listener;
		}

		@Override
		public void onChatroomsChange() {
			CallbackHandler.uiThread.post(new Runnable() {
				@Override
				public void run() {
					listener.onChatroomsChange();
				}
			});

		}

	}
}
