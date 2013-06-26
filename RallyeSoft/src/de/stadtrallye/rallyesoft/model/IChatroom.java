package de.stadtrallye.rallyesoft.model;

import de.rallye.model.structures.PictureSize;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

public interface IChatroom {

	enum ChatStatus { Ready, Refreshing, Posting, Offline }
	
	int getID();
	String getName();
	
	void refresh();
	void saveCurrentState(int lastRead);
	
	void addListener(IChatListener l);
	void removeListener(IChatListener l);
	ChatStatus getChatStatus();
	
	void provideChats();
	void postChat(String msg, Integer pictureID);

	void addChat(ChatEntry chat);
	
	IPictureGallery getPictureGallery(int initialPictureId);
}
