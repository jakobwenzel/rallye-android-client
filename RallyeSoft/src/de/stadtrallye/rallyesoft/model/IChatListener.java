package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.IChatroom.ChatStatus;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

public interface IChatListener {
	
	public void addedChats(List<ChatEntry> entries);
	public void onChatStatusChanged(ChatStatus newStatus);
}
