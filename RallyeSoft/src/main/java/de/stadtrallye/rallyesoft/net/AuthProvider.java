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

package de.stadtrallye.rallyesoft.net;

import android.util.Base64;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.nio.charset.Charset;

import de.rallye.model.Authentication;
import de.rallye.model.structures.UserAuth;

/**
 * Created by Ramon on 21.09.2014.
 */
public class AuthProvider {

	private static final String THIS = AuthProvider.class.getSimpleName();

	@JsonProperty("groupID") protected Integer groupID;
	@JsonProperty("userAuth") protected UserAuth userAuth;
	protected String groupPassword;

	public AuthProvider() {

	}

	public AuthProvider(Integer groupID, UserAuth userAuth) {
		this.groupID = groupID;
		this.userAuth = userAuth;
	}

	public void setGroupID(int groupID) {
		this.groupID = groupID;
	}

	public Integer getGroupID() {
		return groupID;
	}

	public void setUserAuth(UserAuth userAuth) {
		this.userAuth = userAuth;
	}

	public void setGroupPassword(String groupPassword) {
		this.groupPassword = groupPassword;
	}

	private String getBasicAuth(byte[] username, byte[] password) {
		if (username == null) {
			username = new byte[0];
		}

		if (password == null) {
			password = new byte[0];
		}

		final byte[] usernamePassword = new byte[username.length + password.length + 1];

		System.arraycopy(username, 0, usernamePassword, 0, username.length);
		usernamePassword[username.length] = ':';
		System.arraycopy(password, 0, usernamePassword, username.length+1, password.length);

		return "Basic " + Base64.encodeToString(usernamePassword, Base64.DEFAULT);
	}

	public String getUserAuthString() {
		Charset cset = Charset.forName("iso-8859-1");

		byte[] username = userAuth.getHttpUser(groupID).getBytes(cset);
		byte[] password = userAuth.password.getBytes(cset);

		return getBasicAuth(username, password);
	}

	public Authenticator getAuthenticator() {
		return new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				String realm = getRequestingPrompt();
				if (realm.equals(Authentication.USER_AUTH)) {
					Log.i(THIS, "Using Fallback-UserAuthentication");
					return new PasswordAuthentication(userAuth.getHttpUser(groupID), userAuth.password.toCharArray());
				} else if (realm.equals(Authentication.GROUP_AUTH)) {
					Log.i(THIS, "Using Fallback-GroupAuthentication");
					return new PasswordAuthentication(String.valueOf(groupID), groupPassword.toCharArray());
				} else {
					return null;
				}
			}
		};
	}

	/**
	 * equivalent to is logged in
	 * (as far as this client knows)
	 * @return if the client has already the necessary authentication
	 */
	public boolean hasUserAuth() {
		return userAuth != null;
	}

	public boolean hasGroupAuth() {
		return groupPassword != null && groupPassword.length()>=3 && groupID != null;
	}

	public String getGroupPassword() {
		return groupPassword;
	}

	public String getGroupAuthString() {
		Charset cset = Charset.forName("iso-8859-1");

		byte[] username = String.valueOf(groupID).getBytes(cset);
		byte[] password = groupPassword.getBytes(cset);

		return getBasicAuth(username, password);
	}
}

