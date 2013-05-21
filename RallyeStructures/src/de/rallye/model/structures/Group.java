package de.rallye.model.structures;

public class Group {
	
	public final int ID;
	public final String name;
	public final String description;
	
	public Group(int ID, String name, String description) {
		this.ID = ID;
		this.name = name;
		this.description = description;
	}
	
//	/**
//	 * JAXB
//	 */
//	@Deprecated
//	public Group() {
//		ID = 0;
//		name = description = null;
//	}

}
