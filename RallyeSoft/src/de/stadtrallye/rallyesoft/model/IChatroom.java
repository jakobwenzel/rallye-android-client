package de.stadtrallye.rallyesoft.model;

public interface IChatroom {
	
	enum ChatStatus { Ready, Refreshing, Posting, Offline };
	
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
	String getUrlFromImageId(int pictureID, char size);
	
	void onDestroy();
	void onStop();
}
