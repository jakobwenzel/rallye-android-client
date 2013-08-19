package de.stadtrallye.rallyesoft.uimodel;

import de.rallye.model.structures.Task;

/**
 * Interface to control the tasks-map
 * intended for marking single tasks on the map in combination with a pagers
 */
public interface ITasksMapControl {

	void setTask(Task task);
}
