package de.stadtrallye.rallyesoft.model.structures;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import de.rallye.model.structures.LatLng;

/**
 * Created by Ramon on 06.08.13.
 */
@Deprecated
public class Task extends de.rallye.model.structures.Task implements Serializable {

	public Task(int taskID, boolean locationSpecific, LatLng location, String name, String description, boolean multipleSubmits, int submitType) {
		super(taskID, locationSpecific, location, name, description, multipleSubmits, submitType);
	}

//	@Override
//	public int describeContents() {
//		return 0;
//	}
//
//	@Override
//	public void writeToParcel(Parcel d, int flags) {
//		d.writeInt(taskID);
//		d.writeString(name);
//		d.writeString(description);
//		d.writeBooleanArray(new boolean[]{locationSpecific, multipleSubmits});
//		d.writeInt(submitType);
//		d.writeDoubleArray((location != null)? new double[]{location.latitude, location.longitude} : new double[]{Double.NaN,Double.NaN});
//	}
//
//	public static final Parcelable.Creator<Task> CREATOR = new Creator<Task>() {
//		@Override
//		public Task createFromParcel(Parcel source) {
//
//		}
//
//		@Override
//		public Task[] newArray(int size) {
//			return new Task[size];
//		}
//	}
}
