package de.rallye.model.structures;

/**
 * 
 * @author Ramon
 * @version 1.0
 */
public class ChatEntry {
	
	public final int chatID;
	public final int groupID;
	public final long timestamp;
	public final String message;
	public final int pictureID;
	public final int userID;

	public ChatEntry(int chatID, String message, long timestamp, int groupID, int userID, int pictureID) {
		this.message = message;
		this.timestamp = timestamp;
		this.groupID = groupID;
		this.userID = userID;
		this.pictureID = pictureID;
		this.chatID = chatID;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChatEntry other = (ChatEntry) obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (pictureID != other.pictureID)
			return false;
		if (userID != other.userID)
			return false;
		if (groupID != other.groupID)
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return message +"|"+timestamp+"|"+userID+"@"+groupID+"|"+pictureID;
	}
}
