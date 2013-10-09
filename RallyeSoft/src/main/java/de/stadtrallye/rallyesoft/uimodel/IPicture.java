package de.stadtrallye.rallyesoft.uimodel;

import android.net.Uri;

import java.io.Serializable;

/**
* Created by Ramon on 09.10.13.
*/
public interface IPicture extends Serializable {
	Uri getPath();
	String getHash();
}
