package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IConnectionStatusListener;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13.
 */
public class AssistantCompleteFragment extends SherlockFragment implements View.OnClickListener, IConnectionStatusListener {

	private IConnectionAssistant assistant;
	private Button next;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_complete, container, false);
		next = (Button) v.findViewById(R.id.next);

		next.setOnClickListener(this);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			assistant = (IConnectionAssistant) getActivity();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IConnectionAssistant");
		}

		IModel model = assistant.getModel();

		model.addListener(this);
		model.login(assistant.getLogin());
	}

	@Override
	public void onClick(View v) {
		assistant.finish();
	}

	@Override
	public void onConnectionStatusChange(IModel.ConnectionStatus status) {
		if (status == IModel.ConnectionStatus.Connected) {
			next.setEnabled(true);
		}
	}

	@Override
	public void onConnectionFailed(Exception e, IModel.ConnectionStatus lastStatus) {
		Toast.makeText(getActivity(), R.string.invalid_login, Toast.LENGTH_SHORT).show();
		assistant.back();
	}
}
