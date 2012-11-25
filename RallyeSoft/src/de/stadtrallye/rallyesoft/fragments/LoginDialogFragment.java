package de.stadtrallye.rallyesoft.fragments;

import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.R.string;
import de.stadtrallye.rallyesoft.communications.PushService;
import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.exceptions.HttpResponseException;
import de.stadtrallye.rallyesoft.exceptions.RestException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class LoginDialogFragment extends DialogFragment {
	
//	public interface DialogListener {
//		public void onFinish(String url, int group, String password);
//	}
//	
//	private DialogListener listener;
//	
//	@Override
//	public void onAttach(Activity activity) {
//		super.onAttach(activity);
//		listener = (DialogListener) activity;
//	}
	
	private SharedPreferences pref;
	private EditText server;
	private EditText group;
	private EditText pw;
	
	public LoginDialogFragment(SharedPreferences pref) {
		this.pref = pref;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.login)
        		.setView(getActivity().getLayoutInflater().inflate(R.layout.dialog_login, null))
        		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int id) {
        				String newServer = server.getText().toString();
        				
//        				try {
//							RallyePull.testConnection(newServer);
//						} catch (HttpResponseException e) {
//							Log.w("ServerTest", "Server not compatible: " +e.toString());
//							Toast.makeText(getActivity().getApplicationContext(), "Server not compatible", Toast.LENGTH_SHORT).show();
//						} catch (RestException e) {
//							Log.w("ServerTest", "Server not available: " +e.toString());
//							Toast.makeText(getActivity().getApplicationContext(), "Server not available", Toast.LENGTH_SHORT).show();
//						}
        				
        				//TODO: Check if login works
        				SharedPreferences.Editor edit = pref.edit();
                        edit.putString("server", newServer);
                        edit.putInt("group", Integer.parseInt(group.getText().toString()));
                        edit.putString("password", pw.getText().toString());
                        edit.putBoolean("firstLaunch", false);
                        edit.commit();
                        
                        PushService.ensureRegistration(getActivity());
        			}
        		})
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   Toast.makeText(getActivity().getApplicationContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                   }
               });
        AlertDialog dialog = builder.create();
        
        return dialog;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		Dialog dialog = getDialog();
		
		server = (EditText) dialog.findViewById(R.id.server);
        group = (EditText) dialog.findViewById(R.id.group);
        pw = (EditText) dialog.findViewById(R.id.password);
        
		server.setText(pref.getString("server", Config.server));
		group.setText(Integer.toString(pref.getInt("group", Config.group)));
		pw.setText(pref.getString("password", Config.password));
	}
}
