package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class ServerLogin implements Parcelable {
	
	private static final String THIS = ServerLogin.class.getSimpleName();
	private static final int version = 2;

	public static final String SERVER = "server";
	public static final String GROUP_ID = "groupID";
	public static final String GROUP_PASSWORD = "groupPassword";
	public static final String VERSION = "version";

	public enum State {Unknown, Validated, Invalidated};
	
	private State valid;
	private long lastValidated;
	
	final public String server;
	final public int groupID;
	final public String groupPassword;
	final public String name;
	
	public ServerLogin(String server, int group, String name, String password) {
		this(server, group, name, password, 0);
	}
	
	public ServerLogin(String server, int group, String name, String groupPassword, long lastValidated) {
		this.server = server;
		this.groupID = group;
		this.groupPassword = groupPassword;
		this.name = name;
		this.lastValidated = lastValidated;
		this.valid = (lastValidated > 0)? State.Validated : State.Unknown;
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
		return hasServer() && groupPassword != null && hasName();
	}
	
	public boolean hasServer() {
		return server != null && server.length() > 3;
	}
	
	public boolean hasName() {
		return name != null && name.length() >= 3;
	}
	
	@Override
	public String toString() {
		return "Server: "+ server +"| "+name+ "@" +groupID+ " pw: " +groupPassword;
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
		if (groupPassword == null) {
			if (other.groupPassword != null)
				return false;
		} else if (!groupPassword.equals(other.groupPassword))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
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
				.put(GROUP_PASSWORD, groupPassword);
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
			
			return new ServerLogin(js.getString(SERVER), js.getInt(GROUP_ID), null, js.getString(GROUP_PASSWORD));
		} catch (JSONException e) {
			throw new Exception("Invalid JSON", e);
		}
	}

	///Parcelable
	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * exclude user, because it is not needed in LoginDialog
	 */
	@Override
	public void writeToParcel(Parcel d, int flags) {
		d.writeString(server);
		d.writeInt(groupID);
		d.writeString(name);
		d.writeString(groupPassword);
		d.writeLong(lastValidated);
	}
	
	public static final Parcelable.Creator<ServerLogin> CREATOR = new Creator<ServerLogin>() {
		
		@Override
		public ServerLogin[] newArray(int size) {
			return new ServerLogin[size];
		}
		
		@Override
		public ServerLogin createFromParcel(Parcel s) {
			return new ServerLogin(s.readString(), s.readInt(), s.readString(), s.readString(), s.readLong());
		}
	};
}
