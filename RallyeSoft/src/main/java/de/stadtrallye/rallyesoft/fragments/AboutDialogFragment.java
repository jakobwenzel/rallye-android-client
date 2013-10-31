package de.stadtrallye.rallyesoft.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;

import de.stadtrallye.rallyesoft.R;

/**
 * Created by Ramon on 16.10.13.
 */
public class AboutDialogFragment extends SherlockDialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View v = inflater.inflate(R.layout.about, null);

		TextView tvGitHub = (TextView) v.findViewById(R.id.about_github);
		TextView tvLibs = (TextView) v.findViewById(R.id.about_libs);
		tvGitHub.setMovementMethod(LinkMovementMethod.getInstance());
		tvLibs.setMovementMethod(LinkMovementMethod.getInstance());

		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.about)
				.setIcon(R.drawable.ic_launcher)
				.setView(v)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dismiss();
					}
				}).create();
	}
}
