package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import de.stadtrallye.rallyesoft.common.Std;

/**
 * Created by Ramon on 02.10.13.
 */
public class ExpandableSplit {

	public ExpandableSplit(Bundle savedInstanceState) {

	}

	public static SharedPreferences getUiPreferences(Context context) {
		return context.getSharedPreferences(Std.CONFIG_UI, Context.MODE_PRIVATE);
	}
}
