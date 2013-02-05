package de.stadtrallye.rallyesoft.model;

import java.util.List;

public interface IChatListener {
	
	public enum ChatStatus { Offline, Online, Refreshing, Posting };
	
	public void addedChats(List<ChatEntry> entries);
	public void onChatStatusChanged(ChatStatus newStatus);
}
