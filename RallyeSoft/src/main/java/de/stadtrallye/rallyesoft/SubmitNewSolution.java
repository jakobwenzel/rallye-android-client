package de.stadtrallye.rallyesoft;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import de.rallye.model.structures.Group;
import de.rallye.model.structures.ServerInfo;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.Task;
import de.stadtrallye.rallyesoft.uimodel.IPictureTakenListener;
import de.stadtrallye.rallyesoft.util.ImageLocation;

/**
 * Created by Ramon on 04.10.13.
 */
public class SubmitNewSolution extends SherlockFragmentActivity implements IModel.IModelListener {

	public static final int REQUEST_CODE = 7;

	private static final String THIS = SubmitNewSolution.class.getSimpleName();
	private IModel model;

	private ImageView imageView;
	private int type;

	private IPictureTakenListener.Picture picture;
	private EditText editText;
	private EditText editNumber;

	private LinearLayout tabPicture;
	private LinearLayout tabText;
	private LinearLayout tabNumber;
	private int taskID;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Layout, Title, ProgressCircle etc.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setTitle(R.string.submit_new_solution);
		setContentView(R.layout.submit_new_solution);

//		DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(true);

		// Initialize Model
		model = Model.getInstance(getApplicationContext());
//		model.addListener(this);


		tabPicture = (LinearLayout) findViewById(R.id.tab_picture);
		tabText = (LinearLayout) findViewById(R.id.tab_text);
		tabNumber = (LinearLayout) findViewById(R.id.tab_number);

		imageView = (ImageView) findViewById(R.id.picture_submission);
		editText = (EditText) findViewById(R.id.text_submission);
		editNumber = (EditText) findViewById(R.id.number_submission);

		TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				invalidateOptionsMenu();
			}
		};

		editText.addTextChangedListener(textWatcher);
		editNumber.addTextChangedListener(textWatcher);

		Intent intent = getIntent();
		type = intent.getIntExtra(Std.SUBMIT_TYPE, 0);
		taskID = intent.getIntExtra(Std.TASK_ID, -1);

		if ((type & Task.TYPE_PICTURE) == 0) {
			tabPicture.setVisibility(View.GONE);
		} else {
			imageView.setClickable(true);
			imageView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ImageLocation.startPictureTakeOrSelect(SubmitNewSolution.this);
				}
			});
		}
		if ((type & Task.TYPE_TEXT) == 0)
			tabText.setVisibility(View.GONE);
		if ((type & Task.TYPE_NUMBER) == 0)
			tabNumber.setVisibility(View.GONE);

		if (type == Task.TYPE_PICTURE) // If there is only a picture to send, open action selection right away
			imageView.post(new Runnable() {
				@Override
				public void run() {
					imageView.callOnClick();
				}
			});
	}

	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem send = menu.add(Menu.NONE, R.id.send_menu, Menu.NONE, R.string.send);
		send.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		send.setIcon(R.drawable.ic_send_now_light);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem send = menu.findItem(R.id.send_menu);
		boolean enable = (type & Task.TYPE_PICTURE) == Task.TYPE_PICTURE && picture != null;
		enable |= ((type & Task.TYPE_TEXT) == Task.TYPE_TEXT && editText.getText().length() > 0);
		enable |= ((type & Task.TYPE_NUMBER) == Task.TYPE_NUMBER && editNumber.getText().length() > 0);
		send.setEnabled(enable);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.send_menu:
				Intent result = new Intent();
				if (picture != null)
					result.putExtra(Std.PIC, picture.getHash());
				String text = editText.getText().toString();
				if (text != null && text.length() > 0)
					result.putExtra(Std.TEXT, text);
				String number = editNumber.getText().toString();
				if (number != null && number.length() > 0)
					result.putExtra(Std.NUMBER, number);
				setResult(RESULT_OK, result);
				model.getTasks().submitSolution(taskID, type, picture, text, number);
				finish();
				//TODO: This is ugly, but for some reason onActivityResult does not get called.
				// Maybe it's because of https://code.google.com/p/android/issues/detail?id=40537
				// we should use push so that the other clients in the group get notified as well
				model.getTasks().refreshSubmissions();
				return true;
			case android.R.id.home: //Up button
				NavUtils.navigateUpFromSameTask(this);
				return true;
			default:
				return false;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		picture = ImageLocation.imageResult(requestCode, resultCode, data, getApplicationContext(), true);

		if (picture != null) {

			ImageLoader.getInstance().displayImage(picture.getPath().toString(), this.imageView);
			invalidateOptionsMenu();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onConnectionStateChange(IModel.ConnectionState newState) {

	}

	@Override
	public void onConnectionFailed(Exception e, IModel.ConnectionState fallbackState) {

	}

	@Override
	public void onServerInfoChange(ServerInfo info) {

	}

	@Override
	public void onAvailableGroupsChange(List<Group> groups) {

	}
}
