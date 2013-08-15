package de.stadtrallye.rallyesoft.model.structures;

import android.util.Log;

import java.util.List;

import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.Submission;

/**
 * Created by Ramon on 14.08.13.
 */
public class Task extends de.rallye.model.structures.Task {

	public static final int SUBMITS_UNKOWN = -1;
	public static final int SUBMITS_NONE = 0;
	public static final int SUBMITS_SOME = 1;
	public static final int SUBMITS_COMPLETE = 2;

	public final int submits;

	public Task(int taskID, boolean locationSpecific, LatLng location, double radius, String name, String description, boolean multipleSubmits, int submitType, String points, List<AdditionalResource> additionalResources, int submits) {
		super(taskID, locationSpecific, location, radius, name, description, multipleSubmits, submitType, points, additionalResources);

		this.submits = submits;
	}

	public static int getSubmitsFromList(List<Submission> submissions, boolean multipleSubmits) {
		Log.v("Task", "Submits: "+ submissions +" + "+ multipleSubmits);

		if (submissions == null || submissions.size() == 0) {
			Log.v("Task", " -> 0");
			return 0;
		} else if (multipleSubmits) {
			Log.v("Task", " -> 1");
			return 1;
		} else {
			Log.v("Task", " -> 2");
			return 2;
		}
	}
}
