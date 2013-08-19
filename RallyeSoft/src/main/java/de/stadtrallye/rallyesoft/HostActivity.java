package de.stadtrallye.rallyesoft;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.fragments.TasksMapFragment;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.ITasks;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.uimodel.IModelActivity;
import de.stadtrallye.rallyesoft.uimodel.ITabActivity;
import de.stadtrallye.rallyesoft.uimodel.ITasksMapControl;
import de.stadtrallye.rallyesoft.uimodel.TabManager;
import de.stadtrallye.rallyesoft.uimodel.TaskPagerAdapter;

import static de.stadtrallye.rallyesoft.uimodel.Util.getDefaultMapOptions;

/**
 * Stupid (hopefully temporary) Activity to encapsulate the TasksPagerFragment
 * (Plan to call the Fragment directly inside MainActivity)
 */
public class HostActivity extends SherlockFragmentActivity implements IModelActivity, ITabActivity {

	private static final String THIS = HostActivity.class.getSimpleName();
	private IModel model;
	private ViewPager pager;
	private TitlePageIndicator indicator;
	private TaskPagerAdapter fragmentAdapter;
	private ITasksMapControl mapControl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.tasks);
		setContentView(R.layout.tasks_pager);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		model = (IModel) getLastCustomNonConfigurationInstance();
		if (model == null)
			model = Model.getInstance(getApplicationContext());

		ITasks tasks = model.getTasks();

		Log.i(THIS, "Creating View");

		pager = (ViewPager) findViewById(R.id.tasks_pager);
		indicator = (TitlePageIndicator) findViewById(R.id.pager_indicator);

		pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin));


		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		Fragment mapFragment = fm.findFragmentByTag(TasksMapFragment.TAG);
		if (mapFragment == null) {
			mapFragment = new TasksMapFragment();

			Bundle args = getDefaultMapOptions(model);
			args.putBoolean(Std.TASK_MAP_MODE_SINGLE, true);
			mapFragment.setArguments(args);
			ft.replace(R.id.map, mapFragment, TasksMapFragment.TAG).commit();
		}

//		mapControl = (ITasksMapControl) mapFragment;

//		if (fragmentAdapter == null) {
			Log.i(THIS, "Instantiating FragmentStateAdapter");
			fragmentAdapter = new TaskPagerAdapter(getSupportFragmentManager(), this, tasks.getTasksCursor(), mapControl);
			pager.setAdapter(fragmentAdapter);
//		}
//		fragmentAdapter.changeCursor(tasks.getTasksCursor());
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

	@Override
	public TabManager getTabManager() {
		return null;
	}


}
