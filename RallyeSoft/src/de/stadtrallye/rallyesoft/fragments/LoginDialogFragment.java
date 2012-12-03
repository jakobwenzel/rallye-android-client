package de.stadtrallye.rallyesoft.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.stadtrallye.rallyesoft.R;

public class LoginDialogFragment extends SherlockDialogFragment {
	
	
	public interface IDialogCallback {
		public void onDialogPositiveClick(LoginDialogFragment dialog, String server, int group, String pw);
	    public void onDialogNegativeClick(LoginDialogFragment dialog);
	}
	
	
	private EditText server;
	private EditText group;
	private EditText pw;
	private IDialogCallback ui;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
	}
	
//	@Override
//	public void onSaveInstanceState(Bundle state) {
//		super.onSaveInstanceState(state);
//		
//		state.
//	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
            // Instantiate the IDialogCallback so we can send events to the host
            ui = (IDialogCallback) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString() + " must implement IDialogCallback");
        }
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.login)
        		.setView(getActivity().getLayoutInflater().inflate(R.layout.dialog_login, null))
        		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int id) {
        				ui.onDialogPositiveClick(LoginDialogFragment.this, server.getText().toString(), Integer.parseInt(group.getText().toString()), pw.getText().toString());
//        				model.login(ui, tag, server.getText().toString(), Integer.parseInt(group.getText().toString()), pw.getText().toString());
        			}
        		})
        		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						ui.onDialogNegativeClick(LoginDialogFragment.this);
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
        
        Bundle b = getArguments();
        
		server.setText(b.getString("server"));
		group.setText(Integer.toString(b.getInt("group")));
		pw.setText(b.getString("password"));
	}
}
