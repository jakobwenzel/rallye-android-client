package de.stadtrallye.rallyesoft.uimodel;

/**
 * An Activity that uses a {@link de.stadtrallye.rallyesoft.uimodel.TabManager} to show some type of menu (preferably the sliding kind) and manages the Fragments for the activity
 */
public interface ITabActivity {

	TabManager getTabManager();
}
