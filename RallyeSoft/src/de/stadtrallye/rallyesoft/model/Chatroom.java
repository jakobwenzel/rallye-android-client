package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.List;

import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.Model.Tasks;
import de.stadtrallye.rallyesoft.model.comm.AsyncRequest;
import de.stadtrallye.rallyesoft.model.comm.Pull.PendingRequest;
import de.stadtrallye.rallyesoft.util.IConverter;

public class Chatroom implements IModel.IChatroom, IAsyncFinished {
	
	// statics
	private static final String THIS = Model.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(THIS);
	
	// members
	private Model model;
	private int id;
	private String name;
	private int lastTime = 0;
	
	private List<IChatListener> listeners = new ArrayList<IChatListener>();
	
	
	// Constructors | Singleton pattern
	public static List<Chatroom> getChatrooms(List<Integer> serverOut, Model model) {
		List<Chatroom> out = new ArrayList<Chatroom>();
		
		for (Integer i: serverOut) {
			out.add(new Chatroom(i, "Chatroom "+ i, model));
		}
		
		return out;
	}
	
	private Chatroom(int id, String name, Model model) {
		this.id = id;
		this.name = name;
		this.model = model;
	}
	
	// Implementation
	@Override
	public int getID() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void adviseUse() {
		if (!model.loggedIn) {
			err.notLoggedIn();
			return;
		}
		try {
			model.startAsyncTask(null, 0, Tasks.CHAT_DOWNLOAD, model.pull.pendingChatRefresh(id, lastTime));
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	private <T> int startAsyncTask(Tasks type, PendingRequest payload, IConverter<String, T> converter) {
		int id = model.getNextTaskId();
		AsyncRequest<T> r = new AsyncRequest<T>(this, id, converter);
		model.callbacks.put(id, new Model.Task<Void, T>(type, r, null));
		
		r.execute(payload);
		
		return id;
	}
	
	private 

	@Override
	public void addListener(IChatListener l) {
		listeners.add(l);
	}

	@Override
	public void removeListener(IChatListener l) {
		listeners.remove(l);
	}

	@Override
	public List<ChatEntry> getChats() {
		// TODO get From DB
		return null;
	}

	@Override
	public void addChat(String msg) {
		addChat(null, 0, msg);
	}
	
	public void addChat(IModelResult<Boolean> ui, int externalTag, String msg) {
		//TODO: save to DB
		if (!model.loggedIn) {
            err.notLoggedIn();
            return;
        }
        try {
                model.startAsyncTask(ui, externalTag, Tasks.CHAT_DOWNLOAD, model.pull.pendingChatPost(id, msg, 0));
        } catch (RestException e) {
                err.restError(e);
        }
	}

	@Override
	public void onAsyncFinished(int tag, AsyncRequest task) {
		// TODO Auto-generated method stub
		
	}
}
