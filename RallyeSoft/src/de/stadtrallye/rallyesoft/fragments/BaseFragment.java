package de.stadtrallye.rallyesoft.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Adds Debug abilities to a Fragment
 * if enableDebugLogging() was called static, attaching, creating, starting, destroying will be logged with the tag THIS
 * @author Ramon
 *
 */
public class BaseFragment extends SherlockFragment {
	
	protected String THIS = null;

	protected static boolean DEBUG = false;
	
	public static void enableDebugLogging() {
		DEBUG = true;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		if (DEBUG)
			Log.v(THIS, this.toString() +" attached to Activity: "+ activity.toString());
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (DEBUG)
			Log.v(THIS, "Created "+ this.toString());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if (DEBUG)
			Log.v(THIS, "Started "+ this.toString());
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (DEBUG)
			Log.v(THIS, "Destroying "+ this.toString());
	}
}
