package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.net.MalformedURLException;
import java.net.URL;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.net.Paths;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13.
 */
public class AssistantServerFragment extends SherlockFragment implements View.OnClickListener, IModel.IServerInfoCallback {

	private IConnectionAssistant assistant;

	private EditText server;
	private ImageView srv_image;
	private TextView srv_name;
	private TextView srv_desc;
	private ImageLoader loader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.assistant_server, container, false);
		server = (EditText) v.findViewById(R.id.server);

		Button next = (Button) v.findViewById(R.id.next);
		next.setOnClickListener(this);

		Button test = (Button) v.findViewById(R.id.test);
		test.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					String s = getServer();
					loader.displayImage(s+ Paths.SERVER_PICTURE, srv_image);
					assistant.getModel().getServerInfo(AssistantServerFragment.this, s);
				} catch (MalformedURLException e) {

				}
			}
		});

		server.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_NEXT) {
					onClick(v);
					handled = true;
				}
				return handled;
			}
		});

		srv_image = (ImageView) v.findViewById(R.id.server_image);
		srv_name = (TextView) v.findViewById(R.id.server_name);
		srv_desc = (TextView) v.findViewById(R.id.server_desc);

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
		if (s != null)
			server.setText(s);
	}

	private String getServer() throws MalformedURLException {
		String s = server.getText().toString();

		if (!s.endsWith("/"))
			s = s+"/";

		new URL(s);

		return s;
	}

	@Override
	public void onClick(View v) {
		try {
			String s = getServer();

			assistant.setServer(s);
			assistant.next();
		} catch (MalformedURLException e) {
			Toast.makeText(getActivity(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
			return;
		}
	}

	@Override
	public void serverInfo(ServerInfo info) {
		srv_name.setText(info.name);
		srv_desc.setText(info.description);
	}
}
