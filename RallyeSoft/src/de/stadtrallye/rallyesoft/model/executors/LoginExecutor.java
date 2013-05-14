package de.stadtrallye.rallyesoft.model.executors;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.exceptions.LoginFailedException;
import de.stadtrallye.rallyesoft.model.Chatroom;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.Login;
import de.stadtrallye.rallyesoft.net.Request;
import de.stadtrallye.rallyesoft.util.JSONConverter;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayConverter;

public class LoginExecutor extends MyRunnable<List<Chatroom>> {
	
	private Model model;
	private Login login;
	private Request loginR;
	private Request logoutR;

	public LoginExecutor(Request loginR, Request logoutR, Login login, Model model) {
		this.loginR = loginR;
		this.logoutR = logoutR;
		this.model = model;
		this.login = login;
	}
	
	public interface Callback {
		void loginResult(LoginExecutor r);
	}

	/**
	 * Converts JSONObject's to Chatrooms
	 * 
	 * used in conjunction with {@link StringedJSONArrayConverter}
	 * @author Ramon
	 */
	private class LoginConverter extends JSONConverter<Chatroom> {
		
		@Override
		public Chatroom doConvert(JSONObject o) throws JSONException {
			int i = o.getInt("chatroom");
			String name;
			
			try {
				name = o.getString("name");
			} catch (Exception e) {
				name = "Chatroom "+ i;
			}
			
			return new Chatroom(i, name, model);
		}
	}
	
	
	private List<Chatroom> tryLogin() throws JSONException, HttpRequestException {
		return loginR.executeJSONArray(new LoginConverter()).toList();
	}
	
	public Login getLogin() {
		return login;
	}

	@Override
	public List<Chatroom> tryRun() throws JSONException, HttpRequestException, LoginFailedException {
		List<Chatroom> res = null;
		try {
			res = tryLogin();
		} catch (Exception e) {}
		if (res == null) {
			logoutR.execute();
			res = tryLogin();
		}
		if (res == null)
			throw new LoginFailedException();
		
		return res;
	}
	
	@Override
	protected void callback() {
		model.loginResult(this);
	}
}
