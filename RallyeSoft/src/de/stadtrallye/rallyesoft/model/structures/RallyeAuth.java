package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import de.rallye.model.structures.UserAuth;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class RallyeAuth extends UserAuth {
	
	final public ServerLogin login;

	public RallyeAuth(int userID, String password, ServerLogin login) {
		super(userID, password);

		this.login = login;
	}
	
	public int getGroupID() {
		return login.groupID;
	}
	
	public String getHttpUser() {
		return super.getHttpUser(login.groupID);
	}

	public char[] getPassword() {
		return password.toCharArray();
	}
	
	public static class AuthConverter extends JSONConverter<RallyeAuth> {
		
		private ServerLogin login;

		public AuthConverter(ServerLogin login) {
			this.login = login;
		}

		@Override
		public RallyeAuth doConvert(JSONObject o) throws JSONException {
			return new RallyeAuth(o.getInt(RallyeAuth.USER_ID), o.getString(RallyeAuth.PASSWORD), login);
		}
	}

}
