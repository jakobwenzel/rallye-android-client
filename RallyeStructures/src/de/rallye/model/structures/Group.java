package de.rallye.model.structures;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Group {
	
	public final int ID;
	public final String name;
	public final String description;
	
	public Group(int ID, String name, String description) {
		this.ID = ID;
		this.name = name;
		this.description = description;
	}

}
