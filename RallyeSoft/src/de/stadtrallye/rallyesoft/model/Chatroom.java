package de.stadtrallye.rallyesoft.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.IChatListener.ChatStatus;
import de.stadtrallye.rallyesoft.model.Model.Tasks;
import de.stadtrallye.rallyesoft.model.comm.AsyncRequest;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayConverter;

public class Chatroom implements IModel.IChatroom, IAsyncFinished {
	
	// statics
	private static final String CLASS = Chatroom.class.getSimpleName();
	private static final ErrorHandling err = new ErrorHandling(CLASS);
	
	// members
	private Model model;
	private int id;
	private String name;
	private long lastTime = 0;
	private long pendingLastTime = 0;
	
	private final String THIS;
	
	private List<IChatListener> listeners = new ArrayList<IChatListener>();
	private ChatStatus status;
	
	
	// Constructors | Singleton pattern
	public static List<Chatroom> getChatrooms(List<Integer> serverOut, Model model) {
		List<Chatroom> out = new ArrayList<Chatroom>();
		
		for (Integer i: serverOut) {
			out.add(new Chatroom(i, "Chatroom "+ i, model));
		}
		
		return out;
	}
	
	Chatroom(int id, String name, Model model) {
		this.id = id;
		this.name = name;
		this.model = model;
		
		THIS = CLASS +" "+ id;
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
		if (!model.isLoggedIn()) {
			err.notLoggedIn();
			return;
		}
		try {
			if (pendingLastTime != 0)
				Log.w(THIS, "Already refreshing Chat");
			pendingLastTime = System.currentTimeMillis();
			model.startAsyncTask(this, Tasks.CHAT_DOWNLOAD, //TODO: Refresh
					model.pull.pendingChatRefresh(id, lastTime / 1000),
					new StringedJSONArrayConverter<ChatEntry>(new ChatEntry.ChatConverter()));
			
			chatStatusChange(ChatStatus.Refreshing);
			
		} catch (RestException e) {
			err.restError(e);
		}
	}
	
	private void chatUpdate(List<ChatEntry> entries) {
		//TODO: write to DB
		
		for (IChatListener l: listeners) {
			l.addedChats(entries);
		}
	}
	
	private void chatStatusChange(ChatStatus newStatus) {
		status = newStatus;
		
		Log.i(THIS, "Status: "+ newStatus);
		
		for (IChatListener l: listeners) {
			l.onChatStatusChanged(newStatus);
		}
	}

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
		return new ArrayList<ChatEntry>();
	}
	
	@Override
	public void saveCurrentState(Serializable additional) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os;
		try {
			os = new ObjectOutputStream(out);
			os.writeObject(additional);
			Log.d(THIS, out.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void addChat(String msg) {
		//TODO: save to DB
		if (!model.isLoggedIn()) {
            err.notLoggedIn();
            return;
        }
        try {
                model.startAsyncTask(this, Tasks.CHAT_POST, model.pull.pendingChatPost(id, msg, 0), null);
                chatStatusChange(ChatStatus.Posting);
        } catch (RestException e) {
                err.restError(e);
        }
	}

	@Override
	public void onAsyncFinished(AsyncRequest request, boolean success) {
		Tasks type = model.runningRequests.get(request);
		
		switch (type) {
		case CHAT_DOWNLOAD:
		case CHAT_REFRESH:
			try {
				if (success){
					List<ChatEntry> res = ((AsyncRequest<List<ChatEntry>>)request).get();
					
					Log.i(THIS, "Received "+ res.size() +" new Chats in Chatroom "+ this.id +" (since "+ this.lastTime +")");
					
					lastTime = pendingLastTime;
					pendingLastTime = 0;
					
					chatUpdate(res);
					chatStatusChange(ChatStatus.Online);
				} else {
					err.asyncTaskResponseError(request.getException());
					chatStatusChange(ChatStatus.Offline);
				}
			} catch (Exception e) {
				err.asyncTaskResponseError(e);
			}
			break;
		case CHAT_POST:
			if (success) {
				chatStatusChange(ChatStatus.Online);
			} else {
				chatStatusChange(ChatStatus.Offline);
			}
			break;
		default:
			Log.e(THIS, "Unknown Task callback: "+ request);
		}
		
		model.runningRequests.remove(request);
	}
}
