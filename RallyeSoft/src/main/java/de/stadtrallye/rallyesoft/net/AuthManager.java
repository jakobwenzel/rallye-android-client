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

package de.stadtrallye.rallyesoft.net;

import android.util.Base64;
import android.util.Log;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.nio.charset.Charset;

import de.stadtrallye.rallyesoft.model.structures.ServerLogin;

/**
 * Created by Ramon on 21.09.2014.
 */
public class AuthManager {

	private static final String THIS = AuthManager.class.getSimpleName();

	private ServerLogin login;

	public String getAuthString() {
		Charset cset = Charset.forName("iso-8859-1");

		byte[] username = login.getHttpUser().getBytes(cset);
		byte[] password = login.getUserPassword().getBytes(cset);
		if (username == null) {
			username = new byte[0];
		}

		if (password == null) {
			password = new byte[0];
		}

		final byte[] usernamePassword = new byte[username.length + password.length + 1];

		System.arraycopy(username, 0, usernamePassword, 0, username.length);
		System.arraycopy(password, 0, usernamePassword, username.length+1, password.length);

		return "Basic " + Base64.encodeToString(usernamePassword, Base64.DEFAULT);
	}

	public Authenticator getAuthenticator() {
		return new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				String realm = getRequestingPrompt();
				if (realm.equals("RallyeAuth")) {//TODO move Literals
					return new PasswordAuthentication(login.getHttpUser(), login.getUserPassword().toCharArray());
				} else if (realm.equals("RallyeNewUser")) {
					Log.i(THIS, "Switching to NewUserAuthentication");
					return new PasswordAuthentication(String.valueOf(login.getGroupID()), login.getGroupPassword().toCharArray());
				} else {
					return null;
				}
			}
		};
	}

	public boolean hasUserAuth() {
		return login.hasUserAuth();
	}
}

