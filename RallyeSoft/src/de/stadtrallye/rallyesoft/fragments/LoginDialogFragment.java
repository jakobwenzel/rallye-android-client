package de.stadtrallye.rallyesoft.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.stadtrallye.rallyesoft.Config;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.async.PushLogin;
import de.stadtrallye.rallyesoft.communications.RallyePull;
import de.stadtrallye.rallyesoft.model.Model;

public class LoginDialogFragment extends SherlockDialogFragment {
	
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
	
	private Model model;
	private EditText server;
	private EditText group;
	private EditText pw;
	private IModelFinished ui;
	private int tag;
	
//	public LoginDialogFragment() {
//		this.pref = getActivity().getSharedPreferences(getResources().getString(R.string.MainPrefHandler), Context.MODE_PRIVATE);
//	}
	
	public LoginDialogFragment(Model model, IModelFinished ui, int tag) {
		this.model = model;
		this.ui = ui;
		this.tag = tag;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.login)
        		.setView(getActivity().getLayoutInflater().inflate(R.layout.dialog_login, null))
        		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int id) {
     
        				model.login(ui, tag, server.getText().toString(), Integer.parseInt(group.getText().toString()), pw.getText().toString());
        				
//        				RallyePull pull = new RallyePull(pref, getActivity());
//        				PushLogin login = new PushLogin(pull, getActivity(), pref, server.getText().toString(), Integer.parseInt(group.getText().toString()), pw.getText().toString());
//        				login.execute();
        			}
        		})
        		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
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
        
		server.setText(model.getServer());
		group.setText(Integer.toString(model.getGroup()));
		pw.setText(model.getPassword());
	}
}
