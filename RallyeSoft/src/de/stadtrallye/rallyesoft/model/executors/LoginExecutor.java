package de.stadtrallye.rallyesoft.model.executors;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.exceptions.HttpRequestException;
import de.stadtrallye.rallyesoft.exceptions.LoginFailedException;
import de.stadtrallye.rallyesoft.model.Chatroom;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.converter.ChatroomConverter;
import de.stadtrallye.rallyesoft.model.structures.RallyeAuth;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.net.Request;
import de.stadtrallye.rallyesoft.util.JSONConverter;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayConverter;

public class LoginExecutor extends MyRunnable<RallyeAuth> {
	
	private Model model;
	private ServerLogin login;
	private Request loginRequest;
	private Request chatroomRequest;
	private List<Chatroom> chatrooms;

	public LoginExecutor(Request loginRequest, Request chatroomRequest, ServerLogin login, Model model) {
		this.loginRequest = loginRequest;
		this.chatroomRequest = chatroomRequest;
		this.model = model;
		this.login = login;
	}
	
	public interface Callback {
		void loginResult(LoginExecutor r);
	}
	
	public ServerLogin getLogin() {
		return login;
	}
	
	public List<Chatroom> getChatrooms(){
		return chatrooms;
	}

	@Override
	public RallyeAuth tryRun() throws JSONException, HttpRequestException, LoginFailedException {
		RallyeAuth auth;
		
		try {
			auth = loginRequest.executeJSONObject(new RallyeAuth.AuthConverter(login));
			if (auth == null) {
				throw new NullPointerException();
			}
			
			chatrooms = chatroomRequest.executeJSONArray(new ChatroomConverter(model));
			if (chatrooms == null) {
				throw new NullPointerException();
			}
			
			return auth;
		} catch (Exception e) {
			throw new LoginFailedException(e);
		}
		
	}
	
	@Override
	protected void callback() {
		model.loginResult(this);
	}
}
