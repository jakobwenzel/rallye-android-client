package de.rallye.model.structures;

public class ServerInfo {

	public static final String NAME = "name";
	public static final String DESCRIPTION = "description";
	
	final public String name;
	final public String description;
	
	public ServerInfo(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
