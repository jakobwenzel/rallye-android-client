package de.stadtrallye.rallyesoft;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

import java.util.ArrayList;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.fragments.AssistantAuthFragment;
import de.stadtrallye.rallyesoft.fragments.AssistantCompleteFragment;
import de.stadtrallye.rallyesoft.fragments.AssistantGroupsFragment;
import de.stadtrallye.rallyesoft.fragments.AssistantServerFragment;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.ServerLogin;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13.
 */
public class ConnectionAssistant extends SherlockFragmentActivity implements IConnectionAssistant {

	private IModel model;

	private ArrayList<FragmentHandler<?>> steps;
	private int step = 1;

	private String server;
	private int groupID;
	private String name;
	private String pass;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Titel und Inhalt
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setTitle(R.string.connection_assistant);
		setContentView(R.layout.connection_assistant);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(true);

		if (savedInstanceState != null) {
			step = savedInstanceState.getInt(Std.STEP);
		}

		model = Model.getInstance(getApplicationContext());

		//Create FragmentHandlers
		steps = new ArrayList<FragmentHandler<?>>();
		steps.add(new FragmentHandler<AssistantServerFragment>("server", AssistantServerFragment.class));
		steps.add(new FragmentHandler<AssistantGroupsFragment>("groups", AssistantGroupsFragment.class));
		steps.add(new FragmentHandler<AssistantAuthFragment>("auth", AssistantAuthFragment.class));
		steps.add(new FragmentHandler<AssistantCompleteFragment>("complete", AssistantCompleteFragment.class));
	}

	/**
	 * Envelops a Fragment, reuses a already existing Fragment otherwise instantiates a new one
	 * @author Ramon
	 *
	 * @param <T> Fragment Type to envelop
	 */
	private class FragmentHandler<T extends Fragment> {

		private String tag;
		private Class<T> clz;
		private Bundle arg;

		public FragmentHandler(String tag, Class<T> clz) {
			this.tag = tag;
			this.clz = clz;
		}

		public void setArguments(Bundle arg) {
			this.arg = arg;
		}

		public Fragment getFragment() {
			Fragment f = getSupportFragmentManager().findFragmentByTag(tag);

			if (f == null) {
				if (arg == null)
					f = Fragment.instantiate(ConnectionAssistant.this, clz.getName());
				else
					f = Fragment.instantiate(ConnectionAssistant.this, clz.getName(), arg);
			}

			return f;
		}

		public String getTag() {
			return tag;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		FragmentHandler<?> f = steps.get(step-1);
		ft.replace(R.id.fragments, f.getFragment(), f.getTag()).commit();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(Std.STEP, step);
	}

	@Override
	public void back() {
		getSupportFragmentManager().popBackStack();
		step--;
	}

	@Override
	public void setNameAndPass(String name, String pass) {
		this.name = name;
		this.pass = pass;
	}

	@Override
	public ServerLogin getLogin() {
		return new ServerLogin(server, groupID, name, pass);
	}

	@Override
	public void next() {
		if (step < steps.size()) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			FragmentHandler<?> f = steps.get(step);
			ft.replace(R.id.fragments, f.getFragment(), f.getTag()).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
			step++;
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		step--;
	}

	@Override
	public void finish() {
		super.finish();
	}

	/**
	 * IModelActivity
	 */
	@Override
	public IModel getModel() {
		return model;
	}

	@Override
	public void setServer(String server) {
		this.server = server;
	}

	@Override
	public String getServer() {
		return server;
	}
	
	@Override
	public void setGroup(int groupID) {
		this.groupID = groupID;
	}

	/**
	 * IProgressUI
	 */
	@Override
	public void activateProgressAnimation() {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	/**
	 * IProgressUI
	 */
	@Override
	public void deactivateProgressAnimation() {
		setSupportProgressBarIndeterminateVisibility(false);
	}

}
