package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

public interface IChatroom {
	
	public enum ChatStatus { Offline, Online, Refreshing, Posting };
	
	public int getID();
	public String getName();
	
	public void adviseUse();
	public void saveCurrentState(int lastRead);
	
	public void addListener(IChatListener l);
	public void removeListener(IChatListener l);
	public ChatStatus getChatStatus();
	
	public List<ChatEntry> getAllChats();
	public void addChat(String msg);
	
	public void onDestroy();
	public void onStop();
}
