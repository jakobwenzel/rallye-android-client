package de.rallye.model.structures;

public class Group {
	
	public static final String GROUP_ID = "groupID";
	public static final String NAME = "name";
	public static final String DESCRIPTION = "description";
	
	public final int groupID;
	public final String name;
	public final String description;
	
	public Group(int groupID, String name, String description) {
		this.groupID = groupID;
		this.name = name;
		this.description = description;
	}

}
