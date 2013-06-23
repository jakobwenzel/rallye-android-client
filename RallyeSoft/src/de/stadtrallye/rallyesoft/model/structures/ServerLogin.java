package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

import de.rallye.model.structures.UserAuth;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class ServerLogin {
	
	private static final String THIS = ServerLogin.class.getSimpleName();
	private static final int version = 2;

	public static final String SERVER = "server";
	public static final String GROUP_ID = "groupID";
	public static final String GROUP_PASSWORD = "groupPassword";
	public static final String VERSION = "version";

	public enum State {Unknown, Validated, Invalidated};
	
	private State valid;
	private long lastValidated;
	
	private URL server;
	private int groupID;
	private String groupPassword;
	private String name;
	private UserAuth userAuth;
	
	public ServerLogin() {
		this.valid = State.Unknown;
	}
	
	public ServerLogin(String server, int group, String name, String groupPassword, long lastValidated, UserAuth userAuth) throws MalformedURLException {
		this.server = new URL(server);
		this.groupID = group;
		this.setGroupPassword(groupPassword);
		this.setName(name);
		this.lastValidated = lastValidated;
		this.valid = (lastValidated > 0)? State.Validated : State.Unknown;
		this.userAuth = userAuth;
	}

	public void setServer(URL server) {
		this.server = server;
	}

	public URL getServer() {
		return server;
	}

	public void setGroupID(int groupID) {
		this.groupID = groupID;
	}

	public int getGroupID() {
		return groupID;
	}

	public String getGroupPassword() {
		return groupPassword;
	}

	public void setGroupPassword(String groupPassword) {
		this.groupPassword = groupPassword;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setUserAuth(UserAuth userAuth) {
		this.userAuth = userAuth;
	}

	public int getUserID() {
		return userAuth.userID;
	}

	public String getHttpUser() {
		return userAuth.getHttpUser(groupID);
	}

	public String getUserPassword() {
		return userAuth.password;
	}
	
	public long getLastValidated() {
		return lastValidated;
	}
	
	public boolean isValid() {
		return valid == State.Validated;
	}
	
	public boolean isInvalid() {
		return valid == State.Invalidated;
	}
	
	public void validated() {
		validated(System.currentTimeMillis());
	}
	
	public void validated(long timestamp) {
		this.lastValidated = timestamp;
		this.valid = State.Validated;
	}
	
	public void invalidate() {
		this.valid = State.Invalidated;
		this.lastValidated = -this.lastValidated;
	}
	
	public boolean isComplete() {
		return hasServer() && groupPassword != null && hasName() && userAuth != null;
	}
	
	public boolean hasServer() {
		return server != null;
	}
	
	public boolean hasName() {
		return getName() != null && name.length() >= 3;
	}
	
	@Override
	public String toString() {
		return "Server: "+ server +"| "+ name + "@" +groupID+ " pw: " + groupPassword;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerLogin other = (ServerLogin) obj;
		if (groupID != other.groupID)
			return false;
		if (getGroupPassword() == null) {
			if (other.getGroupPassword() != null)
				return false;
		} else if (!getGroupPassword().equals(other.getGroupPassword()))
			return false;
		if (getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!getName().equals(other.getName()))
			return false;
		if (server == null) {
			if (other.server != null)
				return false;
		} else if (!server.equals(other.server))
			return false;
		return true;
	}
	
	public String toJSON() {
		JSONObject js = new JSONObject();
		try {
			js.put(VERSION, version)
				.put(SERVER, server)
				.put(GROUP_ID, groupID)
				.put(GROUP_PASSWORD, getGroupPassword());
		} catch (JSONException e) {
			Log.e(THIS, "JSON Generation Failed!", e);
		}
		return js.toString();
	}
	
	public static ServerLogin fromJSON(String json) throws Exception {
		JSONObject js;
		try {
			js = new JSONObject(json);
			if (js.getInt(VERSION) != version) {
				throw new Exception("Incompatible Versions of Login!");
			}
			
			return new ServerLogin(js.getString(SERVER), js.getInt(GROUP_ID), null, js.getString(GROUP_PASSWORD), 0, null);
		} catch (JSONException e) {
			throw new Exception("Invalid JSON", e);
		}
	}

//	///Parcelable
//	@Override
//	public int describeContents() {
//		return 0;
//	}
//
//	/**
//	 * exclude user, because it is not needed in LoginDialog
//	 */
//	@Override
//	public void writeToParcel(Parcel d, int flags) {
//		d.writeString(server.toString());
//		d.writeInt(groupID);
//		d.writeString(getName());
//		d.writeString(getGroupPassword());
//		d.writeLong(lastValidated);
//	}
//
//	public static final Parcelable.Creator<ServerLogin> CREATOR = new Creator<ServerLogin>() {
//
//		@Override
//		public ServerLogin[] newArray(int size) {
//			return new ServerLogin[size];
//		}
//
//		@Override
//		public ServerLogin createFromParcel(Parcel s) {
//			try {
//				return new ServerLogin(s.readString(), s.readInt(), s.readString(), s.readString(), s.readLong());
//			} catch (MalformedURLException e) {
//				throw new RuntimeException(e);
//			}
//		}
//	};

	public static class AuthConverter extends JSONConverter<UserAuth> {

		@Override
		public UserAuth doConvert(JSONObject o) throws JSONException {
			return new UserAuth(o.getInt(UserAuth.USER_ID), o.getString(UserAuth.PASSWORD));
		}
	}
}
