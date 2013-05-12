package de.stadtrallye.rallyesoft.model;

import java.util.List;

import de.stadtrallye.rallyesoft.model.structures.ChatEntry;

public interface IChatroom {
	
	enum ChatStatus { Offline, Ready, Refreshing, Posting, PostSuccessfull, PostFailed };
	
	int getID();
	String getName();
	
	void refresh();
	void saveCurrentState(int lastRead);
	
	void addListener(IChatListener l);
	void removeListener(IChatListener l);
	ChatStatus getChatStatus();
	
	List<ChatEntry> getAllChats();
	void addChat(String msg);
	
	String getUrlFromImageId(int pictureID, char size);
	IPictureGallery getPictureGallery(int initialPictureId);
	
	void onDestroy();
	void onStop();
}
