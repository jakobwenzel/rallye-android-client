package de.stadtrallye.rallyesoft.model;

import java.util.List;

interface IModel {
	
	public List<? extends IChatroom> getChatrooms();
	
	public interface IChatroom {
		
		public int getID();
		public String getName();
		
		public void adviseUse();
		
		public void addListener(IChatListener l);
		public void removeListener(IChatListener l);
		
		public List<ChatEntry> getChats();
		public void addChat(String msg);
	}
	
	public void login(String server, String password, int group);
	public void logout();
	public void checkConnectionStatus();
	
	public String getUrlFromImageId(int id, char size);
	
	public void onDestroy();
	
	public void addListener(IConnectionStatusListener l);
	public void removeListener(IConnectionStatusListener l);
}
