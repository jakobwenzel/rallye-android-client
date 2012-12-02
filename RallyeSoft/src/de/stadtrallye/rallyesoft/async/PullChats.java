package de.stadtrallye.rallyesoft.async;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.communications.RallyePull;

public class PullChats extends AsyncTask<Void, Void, ArrayList<String>> {
	
	private Fragment ui;
	private RallyePull pull;
	
	final private static String err = "Failed to load Messages:: ";
	
	public PullChats(Fragment ui) {
		this.ui = ui;
		pull = RallyePull.getPull(ui.getActivity());
	}

	@Override
	protected ArrayList<String> doInBackground(Void... params) {
		ArrayList<String> messages = null;
		try {
			JSONArray js = pull.pullChats(2, 0);
			messages = new ArrayList<String>();
			JSONObject next;
			int i = 0;
			while ((next = (JSONObject) js.opt(i)) != null)
			{
				++i;
				messages.add(next.getString("message"));
			}
		} catch (Exception e) {
			Log.e("PullChat", err +e.toString());
			e.printStackTrace();
		}
		return messages;
	}
	
	@Override
	protected void onPostExecute(ArrayList<String> lines) {
		if (lines == null)
			return;
		try {
			View view = ui.getView();
			ListView chats = (ListView) view.findViewById(R.id.chat_list);
	        ArrayAdapter<String> chatAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, lines);
	        chats.setAdapter(chatAdapter);
			
		} catch (Exception e) {
			Log.e("PullChat", "Unkown Error:: " +e.toString());
			e.printStackTrace();
		}
	}

}
