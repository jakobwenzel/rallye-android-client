package de.stadtrallye.rallyesoft.model;

import java.util.List;

public interface IChatListener {
	
	public void addedChats(List<ChatEntry> entries);
}
