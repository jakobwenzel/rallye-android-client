package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.fragments.ChatsFragment;
import de.stadtrallye.rallyesoft.fragments.GameMapFragment;
import de.stadtrallye.rallyesoft.fragments.OverviewFragment;
import de.stadtrallye.rallyesoft.fragments.TasksOverviewFragment;
import de.stadtrallye.rallyesoft.fragments.TasksPagerFragment;
import de.stadtrallye.rallyesoft.fragments.TurnFragment;
import de.stadtrallye.rallyesoft.fragments.WaitForModelFragment;
import de.stadtrallye.rallyesoft.fragments.WelcomeFragment;
import de.stadtrallye.rallyesoft.model.IModel;

/**
 * Contains and manages / executes all Fragments of MainActivity that are uses as Tabs (-> fullsized)
 * Checks for connection with server for certain Tabs, shows Title, sets indicator, contains Toast for Bug-Reports
 * Future use includes Sub-Tab support
 */
public class RallyeTabManager extends TabManager implements AdapterView.OnItemClickListener {

	public static final int TAB_WELCOME = 0;
	public static final int TAB_OVERVIEW = 1;
	public static final int TAB_MAP = 2;
	public static final int TAB_TASKS = 3;
	public static final int TAB_NEXT_MOVE = 4;
	public static final int TAB_CHAT = 5;
	public static final int TAB_WAIT_FOR_MODEL = 6;
	public static final int TAB_TASKS_DETAILS = 100;

	public static final int[] menu = {TAB_OVERVIEW, TAB_CHAT, TAB_NEXT_MOVE, TAB_TASKS, TAB_MAP};

	private final FragmentActivity activity;
	protected IModel model;
	private final ActionBarDrawerToggle drawerToggle;
	private final DrawerLayout drawerLayout;
	private final ListView dashboard;
	private final MenuAdapter dashAdapter;

	public RallyeTabManager(FragmentActivity activity, IModel model, DrawerLayout drawerLayout) {
		super(activity, activity.getSupportFragmentManager(), R.id.content_frame);

		setDefaultTab(TAB_OVERVIEW);

		this.activity = activity;
		this.model = model;
		this.drawerLayout = drawerLayout;

		drawerToggle = new ActionBarDrawerToggle(activity, drawerLayout, R.drawable.ic_drawer_light, R.string.menu_drawer_open, R.string.menu_drawer_close) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				RallyeTabManager.this.activity.getActionBar().setTitle(activeTab.titleId);
				RallyeTabManager.this.activity.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				RallyeTabManager.this.activity.getActionBar().setTitle(R.string.dash_menu);
				RallyeTabManager.this.activity.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
//				dashboard.inv
			}
		};

		// Set the drawer toggle as the DrawerListener
		drawerLayout.setDrawerListener(drawerToggle);


		tabs.put(TAB_WELCOME, new Tab<WelcomeFragment>("welcome", WelcomeFragment.class, R.string.welcome, false));
		tabs.put(TAB_OVERVIEW, new Tab<OverviewFragment>("overview", OverviewFragment.class, R.string.overview, false));
		tabs.put(TAB_MAP, new Tab<GameMapFragment>("map", GameMapFragment.class, R.string.map, false));
		tabs.put(TAB_TASKS, new Tab<TasksOverviewFragment>("tasks", TasksOverviewFragment.class, R.string.tasks, false));
		tabs.put(TAB_NEXT_MOVE, new Tab<TurnFragment>("next_move", TurnFragment.class, R.string.next_move, false));
		tabs.put(TAB_CHAT, new Tab<ChatsFragment>("chat", ChatsFragment.class, R.string.chat, true));
		tabs.put(TAB_WAIT_FOR_MODEL, new Tab<WaitForModelFragment>("waitForModel", WaitForModelFragment.class, R.string.waiting_for_model, false));

		tabs.put(TAB_TASKS_DETAILS, new Tab<TasksPagerFragment>("tasks_details", TasksPagerFragment.class, R.string.tasks, false));



		List<String> nav = new ArrayList<String>();
		for (int i: menu) {
			nav.add(activity.getString(tabs.get(i).titleId));
		}

		dashboard = (ListView) activity.findViewById(R.id.left_drawer);
		dashAdapter = new MenuAdapter(activity, nav);
		dashboard.setAdapter(dashAdapter);
		dashboard.setOnItemClickListener(this);
	}

	public void setModel(IModel model) {
		this.model = model;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switchToTab(menu[position]);
		drawerLayout.closeDrawer(dashboard);
	}

	@Override
	protected boolean checkCondition(Tab tab) {
		return !tab.requiresOnline || model.isConnected();
	}

	@Override
	protected void setSubMode(boolean subTab) {
		drawerToggle.setDrawerIndicatorEnabled(!subTab);
		drawerLayout.setDrawerLockMode((subTab)? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
	}

	@Override
	public boolean onAndroidHome() {
		if (super.onAndroidHome())
			return true;

		drawerToggle.onOptionsItemSelected(new HomeMenuItem());

		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onPostCreate() {
		// Sync the toggle state after onRestoreInstanceState has occurred.
		drawerToggle.syncState();
	}

	@Override
	public void restoreState(Bundle state) {
		super.restoreState(state);

		if (currentTab == null)
			currentTab = (model.isEmpty())? TAB_WELCOME : TAB_OVERVIEW;

		setSubMode(parentTab != null);
	}

	@Override
	protected void showSelectedTab(int key, Tab<?> tab) {
//		activity.getSlidingMenu().setSelectedView(getSelectedView(currentTab));
		activity.setTitle(tab.titleId);
	}

	public void conditionChange() {
		if (!checkCondition(tabs.get(currentTab)))
			switchToTab(TAB_OVERVIEW);

		dashAdapter.notifyDataSetChanged();
	}

	public boolean isMenuOpen() {
		return drawerLayout.isDrawerOpen(dashboard);
	}

//	private View getSelectedView(int pos) {
//		View v = ((ListView)activity.findViewById(R.id.left_drawer)).getChildAt(pos);
//		Log.d(THIS, "SlidingSelector on "+ v);
//		return v;
//	}

	@Override
	protected void switchFailedCondition() {
		Toast.makeText(context, context.getString(R.string.need_connection), Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void switchFailedNotSupported() {
		Toast.makeText(context, context.getString(R.string.unsupported_link), Toast.LENGTH_SHORT).show();
	}

	private class MenuAdapter extends ArrayAdapter<String> {

		public MenuAdapter(Context context, List<String> menu) {
			super(context, R.layout.dashboard_item, android.R.id.text1, menu);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView v = (TextView) super.getView(position, convertView, parent);
			if (isEnabled(position)) {
				v.setTextColor(0xFFFFFFFF);
			} else {
				v.setTextColor(0xFF777777);
			}
			return v;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return checkCondition(tabs.get(menu[position]));
		}
	}
}
