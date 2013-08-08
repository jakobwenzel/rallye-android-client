package de.stadtrallye.rallyesoft.uimodel;

import android.content.Context;
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
//	private final int subTabStart = 100;
	protected Integer currentTab = null;
	protected Tab<?> activeTab = null;

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
		if (state != null)
			currentTab = state.getInt(Std.TAB, defaultTab);
	}

	public void saveState(Bundle outState) {
		outState.putInt(Std.TAB, currentTab);
	}

	public void setArguments(int key, Bundle args) {
		tabs.get(key).setArguments(args);
	}

	/**
	 * Envelops a Fragment, reuses a already existing Fragment otherwise instantiates a new one
	 * @author Ramon
	 *
	 * @param <C> Fragment Type to envelop
	 */
	public class Tab<C extends Fragment> {

		public final String tag;
		private final Class<C> clz;
		public final int title;
		private Bundle args;
		public final boolean requiresOnline;

		public Tab(String tag, Class<C> clz, int title, boolean requiresOnline) {
			this.tag = tag;
			this.clz = clz;
			this.requiresOnline = requiresOnline;
			this.title = title;
		}

		public void setArguments(Bundle arg) {
			this.args = arg;
		}

		public Bundle getArguments() {
			return args;
		}

		public Fragment getFragment() {
			Fragment f = fragmentManager.findFragmentByTag(tag);

			if (f == null) {
				if (args == null)
					f = Fragment.instantiate(context, clz.getName());
				else
					f = Fragment.instantiate(context, clz.getName(), args);
			}

			return f;
		}
	}

	public void showTab() {
		if (tabs.get(currentTab) != activeTab)
			switchToTab(currentTab);
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

//	public boolean switchToSubTab(int key) {
//		Tab<?> tab = tabs.get(key);
//
//		try {
//			startTransaction(tab)
//				.addToBackStack(null)
//			.commit();
//
//			setActiveTab(key, tab);
//
//			return true;
//		} catch (Exception e) {
//			return false;
//		}
//	}

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
