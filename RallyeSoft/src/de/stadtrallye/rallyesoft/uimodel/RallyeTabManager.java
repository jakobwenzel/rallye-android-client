package de.stadtrallye.rallyesoft.uimodel;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.fragments.ChatsFragment;
import de.stadtrallye.rallyesoft.fragments.GameMapFragment;
import de.stadtrallye.rallyesoft.fragments.OverviewFragment;
import de.stadtrallye.rallyesoft.fragments.TasksOverviewFragment;
import de.stadtrallye.rallyesoft.fragments.TasksPagerFragment;
import de.stadtrallye.rallyesoft.fragments.TurnFragment;
import de.stadtrallye.rallyesoft.fragments.WelcomeFragment;
import de.stadtrallye.rallyesoft.model.IModel;

/**
 * Contains and manages / executes all Fragments of MainActivity that are uses as Tabs (-> fullsized)
 * Checks for connection with server for certain Tabs, shows Title, sets indicator, contains Toast for Bug-Reports
 * Future use includes Sub-Tab support
 */
public class RallyeTabManager extends TabManager {

	public static final int TAB_WELCOME = 0;
	public static final int TAB_OVERVIEW = 1;
	public static final int TAB_MAP = 2;
	public static final int TAB_TASKS = 3;
	public static final int TAB_NEXT_MODE = 4;
	public static final int TAB_CHAT = 5;
	public static final int TAB_TASKS_DETAILS = 100;

	private final FragmentActivity activity;
	protected final IModel model;

	public RallyeTabManager(FragmentActivity activity, IModel model) {
		super(activity, activity.getSupportFragmentManager(), R.id.content_frame);

		setDefaultTab(TAB_OVERVIEW);

		this.activity = activity;
		this.model = model;

		tabs.put(0, new Tab<>("welcome", WelcomeFragment.class, R.string.welcome, false));
		tabs.put(1, new Tab<>("overview", OverviewFragment.class, R.string.overview, false));
		tabs.put(2, new Tab<>("map", GameMapFragment.class, R.string.map, false));
		tabs.put(3, new Tab<>("tasks", TasksOverviewFragment.class, R.string.tasks, false));
		tabs.put(4, new Tab<>("next_move", TurnFragment.class, R.string.next_move, false));
		tabs.put(5, new Tab<>("chat", ChatsFragment.class, R.string.chat, true));

		tabs.put(100, new Tab<>("tasks_details", TasksPagerFragment.class, R.string.tasks, false));
	}


	@Override
	protected boolean checkCondition(Tab tab) {
		return !tab.requiresOnline || model.isConnected();
	}

	@Override
	public void restoreState(Bundle state) {
		super.restoreState(state);

		if (currentTab == null)
			currentTab = (model.isEmpty())? TAB_WELCOME : TAB_OVERVIEW;
	}

	@Override
	protected void showSelectedTab(int key, Tab<?> tab) {
//		activity.getSlidingMenu().setSelectedView(getSelectedView(currentTab));
		activity.setTitle(tab.title);
	}

	public void conditionChange() {
		if (!checkCondition(tabs.get(currentTab)))
			switchToTab(TAB_OVERVIEW);
	}

	private View getSelectedView(int pos) {
		View v = ((ListView)activity.findViewById(R.id.left_drawer)).getChildAt(pos);
//		Log.d(THIS, "SlidingSelector on "+ v);
		return v;
	}

	@Override
	protected void switchFailedCondition() {
		Toast.makeText(context, context.getString(R.string.need_connection), Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void switchFailedNotSupported() {
		Toast.makeText(context, context.getString(R.string.unsupported_link), Toast.LENGTH_SHORT).show();
	}
}
