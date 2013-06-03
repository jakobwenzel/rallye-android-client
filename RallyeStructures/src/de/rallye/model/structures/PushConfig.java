package de.rallye.model.structures;

public class PushConfig {
	
	public static final String PUSH_ID = "pushID";
	public static final String PUSH_MODE = "pushMode";
	
	final public String pushID;
	final public String pushMode;
	
	public PushConfig() {
		this.pushID = null;
		this.pushMode = null;
	}
	
	public PushConfig(String pushID, String pushMode) {
		this.pushID = pushID;
		this.pushMode = pushMode;
	}
}
