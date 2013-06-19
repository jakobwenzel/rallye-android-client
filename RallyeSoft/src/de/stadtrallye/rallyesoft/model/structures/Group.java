package de.stadtrallye.rallyesoft.model.structures;

import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.util.JSONConverter;

/**
 * Created by Ramon on 19.06.13.
 */
public class Group extends de.rallye.model.structures.Group {


	public Group(int groupID, String name, String description) {
		super(groupID, name, description);
	}

	public static class GroupConverter extends JSONConverter<Group> {

		@Override
		public Group doConvert(JSONObject o) throws JSONException {
			return new Group(o.getInt(Group.GROUP_ID),
					o.getString(Group.NAME),
					o.getString(Group.DESCRIPTION));
		}
	}
}
