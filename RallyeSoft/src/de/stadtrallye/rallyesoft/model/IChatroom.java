package de.stadtrallye.rallyesoft.model;

import de.rallye.model.structures.PictureSize;

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
	void addChat(String msg);
	
	IPictureGallery getPictureGallery(int initialPictureId);
}
