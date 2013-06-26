package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.IChatroom.ChatStatus;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

public interface IChatListener {
	
	public void chatsEdited(List<ChatEntry> chats);
	public void chatsAdded(List<ChatEntry> chats);
	public void onChatStatusChanged(ChatStatus status);
}
