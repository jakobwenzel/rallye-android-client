package de.stadtrallye.rallyesoft.model.executors;

import de.stadtrallye.rallyesoft.model.structures.RallyeAuth;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.net.Request;

public class LoginExecutor<ID> extends JSONObjectRequestExecutor<RallyeAuth, ID> {

	private ServerLogin login;

	public LoginExecutor(Request loginRequest, ServerLogin login, Callback<ID> callback, ID id) {
		super(loginRequest, new RallyeAuth.AuthConverter(login), callback, id);
		
		this.login = login;
	}
	
	public ServerLogin getLogin() {
		return login;
	}
}
