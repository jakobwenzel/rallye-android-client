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

import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.uimodel.IConnectionAssistant;

/**
 * Created by Ramon on 19.06.13.
 */
public class AssistantServerFragment extends SherlockFragment implements View.OnClickListener, IModel.IObjectAvailableCallback<ServerInfo> {

	private IConnectionAssistant assistant;

	private EditText server;
	private ImageView srv_image;
	private TextView srv_name;
	private TextView srv_desc;
	private Button next;
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

		next = (Button) v.findViewById(R.id.next);
		next.setOnClickListener(this);

		final Button test = (Button) v.findViewById(R.id.test);
		test.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				IModel model = assistant.getModel();
				try {
					String s = server.getText().toString();
					assistant.setServer(s);
					loader.displayImage(model.getServerPictureURL(), srv_image);
					model.getServerInfo(AssistantServerFragment.this);
				} catch (MalformedURLException e) {
					Toast.makeText(getActivity(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
				}
			}
		});

		server.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_NEXT) {
					test.callOnClick();
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

	@Override
	public void onClick(View v) {
		try {
			String s = server.getText().toString();

			assistant.setServer(s);
			assistant.next();
		} catch (MalformedURLException e) {
			Toast.makeText(getActivity(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
		}
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
