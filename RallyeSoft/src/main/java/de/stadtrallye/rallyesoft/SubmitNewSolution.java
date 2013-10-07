package de.stadtrallye.rallyesoft;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.Task;
import de.stadtrallye.rallyesoft.net.PushInit;
import de.stadtrallye.rallyesoft.uimodel.RallyeTabManager;

import static de.stadtrallye.rallyesoft.uimodel.Util.getDefaultMapOptions;

/**
 * Created by Ramon on 04.10.13.
 */
public class SubmitNewSolution extends SherlockFragmentActivity implements IModel.IModelListener {

	private IModel model;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Layout, Title, ProgressCircle etc.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setTitle(R.string.submit_new_solution);
		setContentView(R.layout.submit_new_solution);

//		DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(true);

		// Initialize Model
		model = Model.getInstance(getApplicationContext());
		model.addListener(this);

		int type = getIntent().getIntExtra(Std.SUBMIT_TYPE, Integer.MAX_VALUE);
		if ((type & Task.TYPE_PICTURE) == 0)
			findViewById(R.id.tab_picture).setVisibility(View.GONE);
		if ((type & Task.TYPE_TEXT) == 0)
			findViewById(R.id.tab_text).setVisibility(View.GONE);
		if ((type & Task.TYPE_NUMBER) == 0)
			findViewById(R.id.tab_number).setVisibility(View.GONE);

	}

	@Override
	protected void onStart() {
		super.onStart();


	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		model.removeListener(this);
	}

	@Override
	public void onConnectionStateChange(IModel.ConnectionState newState) {

	}

	@Override
	public void onConnectionFailed(Exception e, IModel.ConnectionState fallbackState) {

	}

	@Override
	public void onServerInfoChange(ServerInfo info) {

	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {

	}
}
