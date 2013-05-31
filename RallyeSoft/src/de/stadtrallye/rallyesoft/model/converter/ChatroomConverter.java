package de.stadtrallye.rallyesoft.model.converter;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.model.Chatroom;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.util.JSONConverter;

public class ChatroomConverter extends JSONConverter<Chatroom> {
	
	private Model model;

	public ChatroomConverter(Model model) {
		this.model = model;
	}
	
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
