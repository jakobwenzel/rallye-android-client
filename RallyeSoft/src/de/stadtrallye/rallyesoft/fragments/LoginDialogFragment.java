package de.stadtrallye.rallyesoft.fragments;

import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.R.string;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class LoginDialogFragment extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(new StringBuilder().append("Login using:\n")
        		.append("Server: ").append(Config.server).append("\n")
        		.append("Group: ").append(Config.group).append("\n")
        		.append("PW: ").append(Config.password))
        		.setTitle(R.string.configuration)
        		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int id) {
        			}
        		});
//               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                   public void onClick(DialogInterface dialog, int id) {
//                       // User cancelled the dialog
//                   }
//               });
        // Create the AlertDialog object and return it
        return builder.create();
	}
}
