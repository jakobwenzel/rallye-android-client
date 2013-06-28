package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.net.MalformedURLException;

import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13.
 */
public class AssistantServerFragment extends SherlockFragment implements IModel.IObjectAvailableCallback<ServerInfo> {

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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_server, container, false);

		protocol = (Spinner) v.findViewById(R.id.protocol);
		server = (EditText) v.findViewById(R.id.server);
		port = (EditText) v.findViewById(R.id.port);
		path = (EditText) v.findViewById(R.id.path);

		port.setHint(Std.DEFAULT_PORT);
		path.setHint(Std.DEFAULT_PATH);

//		path.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//			@Override
//			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//				boolean handled = false;
//				if (actionId == EditorInfo.IME_ACTION_GO) {
//					test.callOnClick();
//					handled = true;
//				}
//				return handled;
//			}
//		});

		test = (Button) v.findViewById(R.id.test);
		test.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				IModel model = assistant.getModel();
				try {
					assistant.setServer(getServer());
					loader.displayImage(model.getServerPictureURL(), srv_image);
					model.getServerInfo(AssistantServerFragment.this);
				} catch (MalformedURLException e) {
					Toast.makeText(getActivity(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
				}
			}
		});

		srv_image = (ImageView) v.findViewById(R.id.server_image);
		srv_name = (TextView) v.findViewById(R.id.server_name);
		srv_desc = (TextView) v.findViewById(R.id.server_desc);

		next = (Button) v.findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view) {
				try {
					assistant.setServer(getServer());
					assistant.next();
				} catch (MalformedURLException e) {
					Toast.makeText(getActivity(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
				}
			}
		});

		loader = ImageLoader.getInstance();
		DisplayImageOptions disp = new DisplayImageOptions.Builder()
				.build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getActivity())
				.defaultDisplayImageOptions(disp)
				.build();
		loader.init(config);

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
			String[] parts = s.replaceAll("^(http|https)://(\\w+\\.\\w+):(\\d+)/(\\w+)/?$", "$1;$2;$3;$4").split(";");
			protocol.setSelection(parts[0].equals("http")? 0 : 1);
			port.setText(parts[2]);
			server.setText(parts[1]);
			path.setText(parts[3]);
		}
	}

	private String getServer() {//TODO: check validity per field
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

	@Override
	public void dataAvailable(ServerInfo info) {
		if (info == null) {
			Toast.makeText(getActivity(), R.string.invalid_server, Toast.LENGTH_SHORT).show();
			next.setVisibility(View.GONE);
		} else {
			srv_name.setText(info.name);
			srv_desc.setText(info.description);
			next.setVisibility(View.VISIBLE);
		}
	}
}
