package de.stadtrallye.rallyesoft.model.structures;

import java.util.List;

import de.rallye.model.structures.AdditionalResource;
import de.rallye.model.structures.LatLng;
import de.rallye.model.structures.Submission;

/**
 * Common:Task enhanced by a state of submissions, matching them into categories, see ({@link #getSubmitsFromList(java.util.List, boolean)})
 * Used to show quick overview of which tasks need further attention
 */
public class Task extends de.rallye.model.structures.Task {

	public static final int SUBMITS_UNKNOWN = -1;
	public static final int SUBMITS_NONE = 0;
	public static final int SUBMITS_SOME = 1;
	public static final int SUBMITS_COMPLETE = 2;

	public final int submits;

	public Task(int taskID, boolean locationSpecific, LatLng location, double radius, String name, String description, boolean multipleSubmits, int submitType, String points, List<AdditionalResource> additionalResources, int submits) {
		super(taskID, locationSpecific, location, radius, name, description, multipleSubmits, submitType, points, additionalResources);

		this.submits = submits;
	}

	/**
	 * @param submissions the submissions received from the server
	 * @param multipleSubmits if the task in question does allow multiple submissions
	 * @return {@link #SUBMITS_UNKNOWN} if no response from server yet
	 * 		{@link #SUBMITS_NONE} nothing submitted yet
	 * 		{@link #SUBMITS_SOME} 1 or more submissions for tasks that allow multiple submissions
	 * 		{@link #SUBMITS_COMPLETE} at the moment: 1 submission (only for tasks that do not allow multiple submissions)
	 */
	public static int getSubmitsFromList(List<Submission> submissions, boolean multipleSubmits) {
		if (submissions == null || submissions.size() == 0) {
			return 0;
		} else if (multipleSubmits) {
			return 1;
		} else {
			return 2;
		}
	}
}
