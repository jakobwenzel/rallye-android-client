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

package de.stadtrallye.rallyesoft.uimodel;

import java.net.MalformedURLException;

/**
 * Parent of fragments that guide through the various steps
 */
public interface IConnectionAssistant extends IProgressUI, IModelActivity {

	void next();
	void setServer(String server) throws MalformedURLException;
	String getServer();
	void setGroup(int id);

	void back();

	void setNameAndPass(String name, String pass);

	void finish(boolean acceptNewConnection);

	void login();

	int getGroup();

	String getPass();
}
