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

package de.stadtrallye.rallyesoft.net.retrofit;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.LoginInfo;
import de.rallye.model.structures.ServerInfo;
import de.rallye.model.structures.UserAuth;
import de.stadtrallye.rallyesoft.net.Paths;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by Ramon on 19.09.2014.
 */
public interface RetroCommunicator {

	@GET(Paths.SERVER_INFO)
	void getServerInfo(Callback<ServerInfo> callback);

	@GET(Paths.GROUPS)
	void getAvailableGroups(Callback<List<Group>> callback);

	@PUT(Paths.GROUPS_WITH_ID)
	void login(@Path(Paths.PARAM_GROUP_ID) int groupID, @Body LoginInfo loginInfo, Callback<UserAuth> callback);

//	@GET(Paths.SERVER_STATUS)
//	void getServerStatus(Callback<ServerStatus> callback);

}
