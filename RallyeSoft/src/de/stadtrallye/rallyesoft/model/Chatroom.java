package de.stadtrallye.rallyesoft.model;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import de.stadtrallye.rallyesoft.exceptions.ErrorHandling;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import de.stadtrallye.rallyesoft.model.Model.Tasks;
import de.stadtrallye.rallyesoft.model.comm.AsyncRequest;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.util.StringedJSONArrayConverter;

public class Chatroom implements IChatroom, IAsyncFinished {
	
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
	
	
	
	static List<Chatroom> getChatrooms(String rooms, Model model) {
		List<Chatroom> out = new ArrayList<Chatroom>();
		
		for (String s: rooms.split(";")) {
			try {
				int id = Integer.parseInt(s);
				String name = "Chatroom "+ id;
				out.add(new Chatroom(id, name, model));
			} catch (Exception e) {}
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
					model.getRallyePull().pendingChatRefresh(id, lastTime / 1000),
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
	
	public ChatStatus getChatStatus() {
		return status;
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
	public List<ChatEntry> getAllChats() {
		// TODO get From DB
		return new ArrayList<ChatEntry>();
	}
	
	@Override
	public void saveCurrentState(int lastRead) {
		
	}
	
	@Override
	public void addChat(String msg) {
		//TODO: save to DB
		if (!model.isLoggedIn()) {
            err.notLoggedIn();
            return;
        }
        try {
                model.startAsyncTask(this, Tasks.CHAT_POST, model.getRallyePull().pendingChatPost(id, msg, 0), null);
                chatStatusChange(ChatStatus.Posting);
        } catch (RestException e) {
                err.restError(e);
        }
	}

	@Override
	public void onAsyncFinished(AsyncRequest request, boolean success) {
		Tasks type = model.getRunningRequests().get(request);
		
		switch (type) {
		case CHAT_DOWNLOAD:
		case CHAT_REFRESH:
			try {
				if (success){
					List<ChatEntry> res = ((AsyncRequest<List<ChatEntry>>)request).get();
					
					Log.i(THIS, "Received "+ res.size() +" new Chats in Chatroom "+ this.id +" (since "+ this.lastTime +")");
					
					lastTime = pendingLastTime;
					pendingLastTime = 0;
					
					model.getLogin().validated();
					
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
		
		model.getRunningRequests().remove(request);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		
	}
}
