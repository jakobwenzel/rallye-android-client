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

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.GroupUser;
import de.rallye.model.structures.LoginInfo;
import de.rallye.model.structures.ServerInfo;
import de.rallye.model.structures.ServerLogin;
import de.rallye.model.structures.UserAuth;
import de.stadtrallye.rallyesoft.exceptions.NoServerKnownException;
import de.stadtrallye.rallyesoft.model.chat.ChatManager;
import de.stadtrallye.rallyesoft.model.chat.IChatManager;
import de.stadtrallye.rallyesoft.model.map.IMapManager;
import de.stadtrallye.rallyesoft.model.map.MapManager;
import de.stadtrallye.rallyesoft.model.tasks.ITaskManager;
import de.stadtrallye.rallyesoft.model.tasks.TaskManager;
import de.stadtrallye.rallyesoft.net.AuthProvider;
import de.stadtrallye.rallyesoft.net.Paths;
import de.stadtrallye.rallyesoft.net.PictureIdResolver;
import de.stadtrallye.rallyesoft.net.retrofit.RetroAuthCommunicator;
import de.stadtrallye.rallyesoft.net.retrofit.RetroCommunicator;
import de.stadtrallye.rallyesoft.net.retrofit.RetroFactory;
import de.stadtrallye.rallyesoft.storage.Storage;
import de.stadtrallye.rallyesoft.util.converters.Serialization;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Created by Ramon on 22.09.2014.
 */
@JsonAutoDetect(getterVisibility = NONE)
public class Server extends AuthProvider {

	private static final String THIS = Server.class.getSimpleName();
	private static final List<IServer.ICurrentServerListener> currentServerListeners = new ArrayList<>();
	private static Server currentServer;
	private static RetroFactory retroFactory = new RetroFactory();

	@JsonProperty
	private final String address;
	private final RetroFactory.ServerHandle serverHandle;
	private final RetroCommunicator communicator;
	private final List<IServer.IServerListener> listeners = new ArrayList<>();

	@JsonProperty
	private ServerInfo serverInfo;
	private RetroAuthCommunicator authCommunicator;
	private ITaskManager taskManager;
	private ChatManager chatManager;
	private MapManager mapManager;
	private List<Group> groups;

	private final ReadWriteLock serverLock = new ReentrantReadWriteLock();
	@JsonProperty private String userName;

	public Server(String address) {
		super();
		this.address = address;
		serverHandle = retroFactory.getServer(address, this);
		this.communicator = serverHandle.getPublicApi();
	}

	@JsonCreator
	public Server(@JsonProperty("address") String address, @JsonProperty("groupID") Integer groupID, @JsonProperty("userAuth") UserAuth userAuth, @JsonProperty("serverInfo") ServerInfo serverInfo, @JsonProperty("userName") String userName) {
		super(groupID, userAuth);
		this.address = address;
		this.userName = userName;
		serverHandle = retroFactory.getServer(address, this);
		this.communicator = serverHandle.getPublicApi();
		this.serverInfo = serverInfo;
	}

	public static Server getCurrentServer() {
		if (currentServer == null)
			load();

		return currentServer;
	}

	public static void setCurrentServer(Server server) {
		if (currentServer != null && currentServer.hasUserAuth())
			currentServer.tryLogout();

		currentServer = server;
		try {
			currentServer.save();// maybe async?
		} catch (IOException e) {
			Log.e(THIS, "Failed to save Server", e);
		}

		notifyCurrentServerChanged();
	}

	public static Server load(String json) {
		if (json == null)
			return null;

		ObjectMapper mapper = Serialization.getInstance();
		try {
			return mapper.readValue(json, Server.class);
		} catch (IOException e) {
			Log.e(THIS, "Failed to deserialize Server", e);
			return null;
		}
	}

	public static void load() {
		try {
			ObjectMapper mapper = Serialization.getInstance();

			currentServer = mapper.readValue(Storage.getServerConfigInputStream(), Server.class);
		} catch (FileNotFoundException e) {
			Log.w(THIS, "No previously saved Server!");
		} catch (IOException e) {
			Log.e(THIS, "Cannot load Server", e);
		}
//		notifyCurrentServerChanged();
	}

	private static void notifyCurrentServerChanged() {
		for (IServer.ICurrentServerListener l : currentServerListeners) {
			l.onNewCurrentServer(currentServer);
		}
	}

	@Deprecated
	public static boolean isStillCurrent(Server server, Object handle) {
		return server == currentServer;
	}

	/**
	 * Not thread safe!!!
	 * If you call this from somewhere else as the Android UI Thread, fix it!!!!!
	 *
	 * @param listener listener
	 */
	public static void addListener(IServer.ICurrentServerListener listener) {
		currentServerListeners.add(listener);
	}

	public static void removeListener(IServer.ICurrentServerListener listener) {
		currentServerListeners.remove(listener);
	}

	public ServerInfo getServerInfoCached() {
		serverLock.readLock().lock();
		try {
			return serverInfo;
		} finally {
			serverLock.readLock().unlock();
		}
	}

	public void addListener(IServer.IServerListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeListener(IServer.IServerListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
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
		synchronized (listeners) {
			for (final IServer.IServerListener listener : listeners) {
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
	}

	public void updateServerInfo() {
		communicator.getServerInfo(new Callback<ServerInfo>() {
			@Override
			public void success(ServerInfo serverInfo, Response response) {
				serverLock.writeLock().lock();
				try {
					Server.this.serverInfo = serverInfo;
				} finally {
					serverLock.writeLock().unlock();
				}
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
		synchronized (listeners) {
			serverLock.readLock().lock();
			try {
				for (final IServer.IServerListener listener : listeners) {
					handler = listener.getCallbackHandler();
					if (handler != null) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								serverLock.readLock().lock();
								try {
									listener.onServerInfoChanged(serverInfo);
								} finally {
									serverLock.readLock().unlock();
								}
							}
						});
					} else {
						listener.onServerInfoChanged(serverInfo);
					}
				}
			} finally {
				serverLock.readLock().unlock();
			}
		}
	}

	private void notifyConnectionFailed(final Exception e, final int status) {
		android.os.Handler handler;
		synchronized (listeners) {
			for (final IServer.IServerListener listener : listeners) {
				handler = listener.getCallbackHandler();
				if (handler != null) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							listener.onConnectionFailed(e, status);
						}
					});
				} else {
					listener.onConnectionFailed(e, status);
				}
			}
		}
	}

	private void notifyLoginSuccessful() {
		android.os.Handler handler;
		synchronized (listeners) {
			for (final IServer.IServerListener listener : listeners) {
				handler = listener.getCallbackHandler();
				if (handler != null) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							listener.onLoginSuccessful();
						}
					});
				} else {
					listener.onLoginSuccessful();
				}
			}
		}
	}

	public void login(final LoginInfo loginInfo) {
		if (!hasGroupAuth())
			throw new RuntimeException("No Group auth");//TODO own exception

		communicator.login(groupID, loginInfo, new Callback<UserAuth>() {
			@Override
			public void success(UserAuth userAuth, Response response) {
				setUserAuth(userAuth);
				setUserName(loginInfo.name);
				notifyLoginSuccessful();
			}

			@Override
			public void failure(RetrofitError e) {
				Log.e(THIS, "Login failed", e);
				notifyConnectionFailed(e, e.getResponse().getStatus());
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

	public void tryLogout() {
		try {
			getAuthCommunicator().logout(currentServer.groupID, new Callback<Response>() {
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
	}

	public IChatManager acquireChatManager(Object handle) throws NoServerKnownException {
		if (chatManager == null) {
			chatManager = new ChatManager(getAuthCommunicator(), Storage.getDatabaseProvider());
		}
		return chatManager;
	}

	public void releaseChatManager(Object handle) {

	}

	public ITaskManager acquireTaskManager(Object handle) throws NoServerKnownException {
		if (taskManager == null) {
			taskManager = new TaskManager(getAuthCommunicator(), Storage.getDatabaseProvider());
		}
		return taskManager;
	}

	public void releaseTaskManager(Object handle) {

	}

	public IMapManager acquireMapManager(Object handle) throws NoServerKnownException {
		if (mapManager == null) {
			mapManager = new MapManager(getAuthCommunicator(), Storage.getDatabaseProvider());
		}
		return mapManager;
	}

	public void releaseMapManager(Object handle) {

	}

	public String getAvatarUrl(int groupID) {
		return address + Paths.getAvatar(groupID);
	}

	public GroupUser getUser() {
		return new GroupUser(this.userAuth.userID, this.groupID, this.userName);
	}

	public String getAddress() {
		return address;
	}

	public String getServerIconUrl() {
		return address + Paths.SERVER_PICTURE;
	}

	public List<Group> getAvailableGroupsCached() {
		return groups;
	}

	public String serialize() {
		ObjectMapper mapper = Serialization.getInstance();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			Log.e(THIS, "Could not Serialize Server", e);
			return null;
		}
	}

	public String getPictureUploadURL(String hash) {
		return address + Paths.PICS + "/" + hash;
	}

	public ServerLogin exportLogin() {
		return new ServerLogin(address, groupID, groupPassword);
	}

	private void setUserName(String userName) {
		this.userName = userName;
	}
}
