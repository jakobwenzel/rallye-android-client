package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.util.HashMap;
import java.util.Map;

import de.stadtrallye.rallyesoft.common.Std;

/**
 * Manages all fragments used as Tabs in an activity
 */
public abstract class TabManager {

	protected final Context context;
	private final FragmentManager fragmentManager;
	private final int replaceId;
	protected Integer currentTab = null;
	protected Tab<?> activeTab = null;
	protected Integer parentTab = null;

	protected final Map<Integer, Tab<?>> tabs = new HashMap<>();
	private int defaultTab = 0;

	public TabManager(Context context, FragmentManager fragmentManager, int replaceId) {
		this.context = context;
		this.fragmentManager = fragmentManager;
		this.replaceId = replaceId;
	}


	public void setDefaultTab(int tab) {
		defaultTab = tab;
	}

	public void restoreState(Bundle state) {
		if (state != null) {
			currentTab = state.getInt(Std.TAB, defaultTab);
			parentTab = state.getInt(Std.SUB_TAB, -1);
			if (parentTab == -1)
				parentTab = null;
		}
	}

	public void saveState(Bundle outState) {
		outState.putInt(Std.TAB, currentTab);
		if (parentTab != null)
			outState.putInt(Std.SUB_TAB, parentTab);
	}

	public void setArguments(int key, Bundle args) {
		tabs.get(key).args = args;
	}

	public boolean onAndroidHome() {
		if (parentTab != null) {
			closeSubTab();
			return true;
		} else
			return false;
	}

	public boolean  onBackPressed() {
		if (parentTab != null) {
			closeSubTab();
			return true;
		} else
			return false;
	}

	public abstract void onConfigurationChanged(Configuration newConfig);

	public abstract void onPostCreate();

	public abstract boolean isMenuOpen();

	/**
	 * Envelops a Fragment, reuses a already existing Fragment otherwise instantiates a new one
	 * @author Ramon
	 *
	 * @param <C> Fragment Type to envelop
	 */
	public class Tab<C extends Fragment> {

		public final String tag;
		private final Class<C> clz;
		public final int titleId;
		private Bundle args;
		public final boolean requiresOnline;

		public Tab(String tag, Class<C> clz, int titleId, boolean requiresOnline) {
			this.tag = tag;
			this.clz = clz;
			this.requiresOnline = requiresOnline;
			this.titleId = titleId;
		}

		public Fragment getFragment() {
			Fragment f = fragmentManager.findFragmentByTag(tag);

			if (f == null) {
				f = Fragment.instantiate(context, clz.getName());
				if (args != null)
					f.setArguments(args);
			} else if (args != null) {
				Bundle oldArgs = f.getArguments();
				if (!args.equals(oldArgs))
					oldArgs.putAll(args);
			}

			return f;
		}
	}

	public void showTab() {
		if (tabs.get(currentTab) != activeTab) {
			if (parentTab == null)
				switchToTab(currentTab);
			else {
				int subTab = currentTab;
				currentTab = parentTab;
				openSubTab(subTab, null);
			}
		}
	}

	public boolean switchToTab(int key) {
		Tab<?> tab = tabs.get(key);

		if (tab == null) {
			switchFailedNotSupported();
			return false;
		} else if (tab == activeTab)
			return false;

		try {
			startTransaction(tab)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
			.commit();

			setActiveTab(key, tab);

			return true;
		} catch (Exception e) {
			switchFailedCondition();
			return false;
		}
	}

	protected abstract void switchFailedNotSupported();

	protected abstract void switchFailedCondition();

	public boolean openSubTab(int key, Bundle args) {
		Tab<?> tab = tabs.get(key);
		tab.args = args;

		try {
			startTransaction(tab)
//				.addToBackStack(null)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
			.commit();

			setSubMode(true);
			setActiveTab(key, tab);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean closeSubTab() {
		Tab<?> tab = tabs.get(parentTab);

		try {
			startTransaction(tab)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
			.commit();

			setActiveTab(parentTab, tab);
			setSubMode(false);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected void setSubMode(boolean subTab) {
		if (subTab)
			parentTab = currentTab;
		else
			parentTab = null;
	}

	private FragmentTransaction startTransaction(Tab<?> tab) throws Exception {
		if (!checkCondition(tab))
			throw new Exception();

		FragmentTransaction ft = fragmentManager.beginTransaction();

		ft.replace(replaceId, tab.getFragment(), tab.tag);

		return ft;
	}

	protected void setActiveTab(int key, Tab<?> tab) {
		currentTab = key;
		activeTab = tab;

		showSelectedTab(key, tab);
	}

	protected abstract void showSelectedTab(int key, Tab<?> tab);

	protected abstract boolean checkCondition(Tab<?> tab);
}
