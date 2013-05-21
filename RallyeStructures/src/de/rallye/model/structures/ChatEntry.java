package de.rallye.model.structures;

/**
 * 
 * @author Ramon
 * @version 1.0
 */
public class ChatEntry {
	
	public final int chatID;
	public final int senderID;
	public final int timestamp;
	public final String message;
	public final int pictureID;
	public final boolean self;

	public ChatEntry(int chatID, String message, int timestamp, int senderID, boolean self, int pictureID) {
		this.message = message;
		this.timestamp = timestamp;
		this.senderID = senderID;
		this.self = self;
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
		if (self != other.self)
			return false;
		if (senderID != other.senderID)
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return message +"|"+timestamp+"|"+senderID+"|"+self+"|"+pictureID;
	}
}
