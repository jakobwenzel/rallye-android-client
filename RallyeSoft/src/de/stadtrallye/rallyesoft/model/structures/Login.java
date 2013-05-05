package de.stadtrallye.rallyesoft.model.structures;

import de.stadtrallye.rallyesoft.common.Std;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Parcel;
import android.os.Parcelable;

public class Login implements Parcelable {
	
	public enum State {Unknown, Validated, Invalidated};
	private State valid;
	private long lastValidated;
	
	private String server;
	private int group;
	private String password;
	private String name;
	
	public Login(String server, int group, String name, String password) {
		this(server, group, name, password, 0);
	}
	
	public Login(String server, int group, String name, String password, long lastValidated) {
		this.server = server;
		this.group = group;
		this.password = password;
		this.name = name;
		this.lastValidated = lastValidated;
		this.valid = (lastValidated > 0)? State.Validated : State.Unknown;
	}
	
	
	public String getServer() {
		return server;
	}
	
	public int getGroup() {
		return group;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getName() {
		return name;
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
	
	public void invalidated() {
		this.valid = State.Invalidated;
		this.lastValidated = -this.lastValidated;
	}
	
	public boolean isComplete() {
		return hasServer() && password != null;
	}
	
	public boolean hasServer() {
		return server != null && server.length() > 3;
	}
	
	@Override
	public String toString() {
		return "Server: "+ server +"| "+name+ "@" +group+ " pw: " +password;
	}
	
	public static Login fromPref(SharedPreferences pref) {
		Login l = new Login(pref.getString(Std.SERVER, null),
				pref.getInt(Std.GROUP, 0),
				pref.getString(Std.NAME, null),
				pref.getString(Std.PASSWORD, null),
				pref.getLong(Std.LAST_LOGIN, 0));
		
		if (l.isComplete()) {
			return l;
		} else {
			return null;
		}
	}
	
	/**
	 * SharedPreferences.edit()
	 * @param edit
	 */
	public void toPref(Editor edit) {
		edit.putString(Std.SERVER, server);
		edit.putInt(Std.GROUP, group);
		edit.putString(Std.PASSWORD, password);
		edit.putLong(Std.LAST_LOGIN, lastValidated);
		edit.putString(Std.NAME, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Login other = (Login) obj;
		if (group != other.group)
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
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

	///Parcelable
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel d, int flags) {
		d.writeString(server);
		d.writeInt(group);
		d.writeString(name);
		d.writeString(password);
		d.writeLong(lastValidated);
	}
	
	public static final Parcelable.Creator<Login> CREATOR = new Creator<Login>() {
		
		@Override
		public Login[] newArray(int size) {
			return new Login[size];
		}
		
		@Override
		public Login createFromParcel(Parcel s) {
			return new Login(s.readString(), s.readInt(), s.readString(), s.readString(), s.readLong());
		}
	};
}
