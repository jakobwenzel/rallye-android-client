package de.stadtrallye.rallyesoft.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.net.MalformedURLException;
import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * 1. Page of ConnectionAssistant
 * Asks for Server details and tries the Connection (showing ServerInfo)
 */
public class AssistantServerFragment extends SherlockFragment implements IModel.IModelListener, View.OnClickListener {

	private IConnectionAssistant assistant;

	private EditText server;
	private ImageView srv_image;
	private TextView srv_name;
	private TextView srv_desc;
	private Button next;
	private ImageLoader loader;
    private Spinner protocol;
	private EditText port;
	private Button test;
	private EditText path;
	private ScrollView scrollView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_server, container, false);

		scrollView = (ScrollView)v.findViewById(R.id.scrollView);

		protocol = (Spinner) v.findViewById(R.id.protocol);
		server = (EditText) v.findViewById(R.id.server);
		port = (EditText) v.findViewById(R.id.port);
		path = (EditText) v.findViewById(R.id.path);

		port.setHint(Std.DEFAULT_PORT);
		path.setHint(Std.DEFAULT_PATH);

		path.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					test.callOnClick();
				}
				return false;
			}
		});

		test = (Button) v.findViewById(R.id.test);
		test.setOnClickListener(this);

		srv_image = (ImageView) v.findViewById(R.id.server_image);
		srv_name = (TextView) v.findViewById(R.id.server_name);
		srv_desc = (TextView) v.findViewById(R.id.server_desc);

		next = (Button) v.findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view) {
				assistant.next();
			}
		});

		loader = ImageLoader.getInstance();

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
	}

	@Override
	public void onStart() {
		super.onStart();

		String s = assistant.getServer();
		if (s != null) {
			String[] parts = s.replaceAll("^(http|https)://([0-9A-Za-z_.-]+?):(\\d+?)/(\\w+?)/?$", "$1;$2;$3;$4").split(";");
			protocol.setSelection(parts[0].equals("http")? 0 : 1);
			port.setText(parts[2]);
			server.setText(parts[1]);
			path.setText(parts[3]);
		}

		assistant.getModel().addListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		assistant.getModel().removeListener(this);
	}

	private String getServer() {
		String protocol = this.protocol.getSelectedItem().toString();
		String server = this.server.getText().toString();
		String port = this.port.getText().toString();
		if (port.equals(""))
			port = Std.DEFAULT_PORT;
		String path = this.path.getText().toString();
		if (path.equals(""))
			path = Std.DEFAULT_PATH;

		return protocol +"://"+ server +":"+ port +"/"+ path;
	}

	// "Test"
	@Override
	public void onClick(View v) {
		IModel model = assistant.getModel();
		try {
			String server = getServer();
			assistant.setServer(server);
			loader.displayImage(model.getServerPictureURL(), srv_image);

			View focus = getActivity().getCurrentFocus();
			if (focus != null) {
				InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
//			getActivity().getCurrentFocus().clearFocus();
		} catch (MalformedURLException e) {
			Toast.makeText(getActivity(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onConnectionStateChange(IModel.ConnectionState newState) {

	}

	@Override
	public void onConnectionFailed(Exception e, IModel.ConnectionState fallbackState) {
		if (fallbackState == IModel.ConnectionState.TemporaryNotAvailable) {
			Toast.makeText(getActivity(), R.string.invalid_server, Toast.LENGTH_SHORT).show();
			next.setVisibility(View.GONE);
		}
	}

	@Override
	public void onServerInfoChange(ServerInfo info) {
		srv_name.setText(info.name);
		srv_desc.setText(info.description);
		next.setVisibility(View.VISIBLE);
		getView().post(new Runnable() {
			@Override
			public void run() {
				scrollView.scrollTo(0, next.getTop());
				next.requestFocus();
			}
		});
	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {

	}
}
