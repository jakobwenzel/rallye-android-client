package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.IChatroom.ChatStatus;

public interface IChatListener {
	
	public void addedChats(List<ChatEntry> entries);
	public void onChatStatusChanged(ChatStatus newStatus);
}
