package de.stadtrallye.rallyesoft.async;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.communications.Pull;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PullChats extends AsyncTask<Void, Void, ArrayList<String>> {
	
	private Fragment ui;
	private Pull pull;
	
	public PullChats(Fragment ui, Pull pull) {
		this.ui = ui;
		this.pull = pull;
	}

	@Override
	protected ArrayList<String> doInBackground(Void... params) {
		JSONArray js = pull.getJSONArrayFromRest("/chat/getChatEntries/wertWERTwertWERT");
		ArrayList<String> messages = new ArrayList<String>();
		try {
			JSONObject next;
			int i = 0;
			while ((next = (JSONObject) js.opt(i)) != null)
			{
				++i;
				messages.add(next.getString("message"));
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return messages;
	}
	
	@Override
	protected void onPostExecute(ArrayList<String> lines) {
		try {
			View view = ui.getView();
			ListView chats = (ListView) view.findViewById(R.id.chat_list);
	        ArrayAdapter<String> chatAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, lines);
	        chats.setAdapter(chatAdapter);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
