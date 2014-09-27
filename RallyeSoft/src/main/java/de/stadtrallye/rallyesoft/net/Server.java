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

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.GroupUser;
import de.rallye.model.structures.LoginInfo;
import de.rallye.model.structures.ServerInfo;
import de.rallye.model.structures.UserAuth;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.IServer;
import de.stadtrallye.rallyesoft.model.chat.ChatManager;
import de.stadtrallye.rallyesoft.model.chat.IChatManager;
import de.stadtrallye.rallyesoft.model.map.IMapManager;
import de.stadtrallye.rallyesoft.model.map.MapManager;
import de.stadtrallye.rallyesoft.model.tasks.ITaskManager;
import de.stadtrallye.rallyesoft.model.tasks.TaskManager;
import de.stadtrallye.rallyesoft.net.retrofit.RetroAuthCommunicator;
import de.stadtrallye.rallyesoft.net.retrofit.RetroCommunicator;
import de.stadtrallye.rallyesoft.net.retrofit.RetroFactory;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.util.converters.Serialization;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Ramon on 22.09.2014.
 */
public class Server extends AuthProvider {

	private static final String THIS = Server.class.getSimpleName();

	private static Server currentServer;
	private static RetroFactory retroFactory;

	public static Server getCurrentServer() {
		return currentServer;
	}


	@JsonProperty private final String address;
	@JsonProperty private ServerInfo serverInfo;
	private final RetroFactory.ServerHandle serverHandle;
	private final RetroCommunicator communicator;
	private final List<IServer.IServerListener> listeners = new ArrayList<>();

	private RetroAuthCommunicator authCommunicator;

	private ITaskManager taskManager;
	private ChatManager chatManager;
	private MapManager mapManager;

	private List<Group> groups;

	public Server(String address) {
		super();
		this.address = address;
		serverHandle = retroFactory.getServer(address);
		this.communicator = serverHandle.getPublicApi();
	}

	@JsonCreator
	public Server(@JsonProperty("address") String address, @JsonProperty("groupID") int groupID, @JsonProperty("userAuth") UserAuth userAuth, @JsonProperty("serverInfo") ServerInfo serverInfo) {
		super(groupID, userAuth);
		this.address = address;
		serverHandle = retroFactory.getServer(address);
		this.communicator = serverHandle.getPublicApi();
		this.serverInfo = serverInfo;
	}

	public ServerInfo getServerInfoCached() {
		return serverInfo;
	}

	public void addListener(IServer.IServerListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IServer.IServerListener listener) {
		listeners.remove(listener);
	}

	public void updateAvailableGroups() {
		communicator.getAvailableGroups(new Callback<List<Group>>() {
			@Override
			public void success(List<Group> groups, Response response) {
				Server.this.groups = groups;
				notifyAvailableGroups();
			}

			@Override
			public void failure(RetrofitError e) {
				//TODO Server.commError()
				Log.e(THIS, "unable to get Groups", e);
			}
		});
	}

	private void notifyAvailableGroups() {
		android.os.Handler handler;
		for (final IServer.IServerListener listener: listeners) {
			handler = listener.getCallbackHandler();
			if (handler != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						listener.onAvailableGroupsChanged(groups);
					}
				});
			} else {
				listener.onAvailableGroupsChanged(groups);
			}
		}
	}

	public void updateServerInfo() {
		communicator.getServerInfo(new Callback<ServerInfo>() {
			@Override
			public void success(ServerInfo serverInfo, Response response) {
				Server.this.serverInfo = serverInfo;
				notifyServerInfo();
			}

			@Override
			public void failure(RetrofitError e) {
				//TODO Server.commError()
				Log.e(THIS, "unable to get ServerInfo", e);
			}
		});
	}

	private void notifyServerInfo() {
		android.os.Handler handler;
		for (final IServer.IServerListener listener: listeners) {
			handler = listener.getCallbackHandler();
			if (handler != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						listener.onServerInfoChanged(serverInfo);
					}
				});
			} else {
				listener.onServerInfoChanged(serverInfo);
			}
		}
	}

	public void login(LoginInfo loginInfo, final ILoginListener loginListener) {
		if (!hasGroupAuth())
			throw new RuntimeException("No Group auth");//TODO own exception

		communicator.login(groupID, loginInfo, new Callback<UserAuth>() {
			@Override
			public void success(UserAuth userAuth, Response response) {
				setUserAuth(userAuth);
				loginListener.loginSuccessful();
			}

			@Override
			public void failure(RetrofitError error) {
				loginListener.loginFailed();
			}
		});
	}

	public RetroAuthCommunicator getAuthCommunicator() throws NoServerKnownException {
		if (!hasUserAuth())
			throw new NoServerKnownException("Trying to access the Auth API of a Server without having Auth data");

		if (authCommunicator == null)
			authCommunicator = serverHandle.getAuthApi();
		return authCommunicator;
	}

	public RetroCommunicator getCommunicator() {
		return communicator;
	}

	public PictureIdResolver getPictureResolver() {
		return new PictureIdResolver(address);
	}

	public void save() throws IOException {
		ObjectMapper mapper = Serialization.getInstance();
		mapper.writeValue(Storage.getServerConfigOutputStream(), this);
	}

	public static Server load(String json) {
		ObjectMapper mapper = Serialization.getInstance();
		try {
			return mapper.readValue(json, Server.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void load() {
		try {
			ObjectMapper mapper = Serialization.getInstance();
			Server server = mapper.readValue(Storage.getServerConfigInputStream(), Server.class);

			currentServer = server;
		} catch (FileNotFoundException e) {
			Log.w(THIS, "No previously saved Server!");
		} catch (IOException e) {
			Log.e(THIS, "Cannot load Server", e);
		}
	}

	public static void setCurrentServer(Server server) {
		if (currentServer != null && currentServer.hasUserAuth())
			try {
				currentServer.getAuthCommunicator().logout(currentServer.groupID, new Callback<Response>() {
					@Override
					public void success(Response response, Response response2) {
						Log.d(THIS, "Logged out of old Server");
					}

					@Override
					public void failure(RetrofitError e) {
						Log.e(THIS, "Could not logout old Server", e);
					}
				});
			} catch (NoServerKnownException e) {
				Log.e(THIS, "Could not logout old Server", e);
			}

		currentServer = server;
	}

	public IChatManager acquireChatManager(Object handle) throws NoServerKnownException{
		if (chatManager == null) {
			chatManager = new ChatManager();
		}
		return chatManager;
	}

	public void releaseChatManager(Object handle) {

	}

	public ITaskManager acquireTaskManager(Object handle) throws NoServerKnownException{
		if (taskManager == null) {
			taskManager = new TaskManager();
		}
		return taskManager;
	}

	public void releaseTaskManager(Object handle) {

	}

	public IMapManager acquireMapManager(Object handle) throws NoServerKnownException{
		if (mapManager == null) {
			mapManager = new MapManager();
		}
		return mapManager;
	}

	public void releaseMapManager(Object handle) {

	}

	public String getAvatarUrl(int groupID) {
		return address + Paths.getAvatar(groupID);
	}

	public GroupUser getUser() {
		return null;
	}

	public static boolean isStillCurrent(Server server, Object handle) {
		return server == currentServer;
	}

	public String getAddress() {
		return address;
	}

	public String getServerIconUrl() {
		return address+ Paths.SERVER_PICTURE;
	}

	public List<Group> getAvailableGroupsCached() {
		return groups;
	}

	public String serialize() {
		ObjectMapper mapper = Serialization.getInstance();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getPictureUploadURL(String hash) {
		return address+Paths.PICS+"/"+hash;
	}

	public interface ILoginListener {
		void loginSuccessful();

		void loginFailed();
	}
}
