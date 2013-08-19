package de.stadtrallye.rallyesoft.model;

import java.io.Serializable;

import de.rallye.model.structures.PictureSize;
import de.stadtrallye.rallyesoft.net.Paths;

/**
 * Serializable Class to generate Picture URLs (as String) from a pictureID and PictureSize
 * So PictureGallery's do not have to hold on to Model, which is not serializable
 */
public class PictureIdResolver implements Serializable {

	private final String base;

	public PictureIdResolver(String base) {
		this.base = base;
	}

	public String resolvePictureID(int id, PictureSize size) {
		return base + Paths.getPic(id, size);
	}
}
