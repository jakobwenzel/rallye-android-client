package de.stadtrallye.rallyesoft;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import de.stadtrallye.rallyesoft.fragments.TasksPagerFragment;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;

/**
 * Stupid (hopefully temporary) Activity to encapsulate the TasksPagerFragment
 * (Plan to call the Fragment directly inside MainActivity)
 */
public class HostActivity extends SherlockFragmentActivity implements IModelActivity {

	private IModel model;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.tasks);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		model = (IModel) getLastCustomNonConfigurationInstance();
		if (model == null)
			model = Model.getModel(getApplicationContext());

		FragmentManager fm = getSupportFragmentManager();
		Fragment f = fm.findFragmentByTag("tasks_details");
		if (f == null) {
			f = new TasksPagerFragment();
		}
		Bundle args = getIntent().getExtras();
		f.setArguments(args);
		fm.beginTransaction().replace(android.R.id.content, f, "tasks_details").commit();
	}



	@Override
	public IModel getModel() {
		return model;
	}

	/**
	 * Called if e.g. the App is rotated
	 * => save the model
	 */
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return model;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return false;
	}
}
