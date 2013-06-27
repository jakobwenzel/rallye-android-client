package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.IChatroom.ChatStatus;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

public interface IChatListener {

	/**
	 * Swap each existing chat with its replacement (Identified by chatID)
	 */
	public void chatsEdited(List<ChatEntry> chats);

	/**
	 * Add these chats to the existing ones
	 */
	public void chatsAdded(List<ChatEntry> chats);

	/**
	 * Callback for IChatroom.provideChats()
	 * @param chats all available Chats
	 */
	public void chatsProvided(List<ChatEntry> chats);

	public void onChatStatusChanged(ChatStatus status);
}
