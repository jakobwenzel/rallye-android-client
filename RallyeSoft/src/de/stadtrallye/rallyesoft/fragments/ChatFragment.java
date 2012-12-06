package de.stadtrallye.rallyesoft.fragments;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import de.stadtrallye.rallyesoft.IModelActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.ChatEntry;
import de.stadtrallye.rallyesoft.model.IModelResult;
import de.stadtrallye.rallyesoft.model.JSONTranslator;
import de.stadtrallye.rallyesoft.model.Model;

public class ChatFragment extends SherlockFragment implements IModelResult<JSONArray> {
	
	final static private int TASK_SIMPLE_CHAT = 101;
	
	private Model model;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		Log.v("ChatFragment", "ChatFragment created");
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		try {
			model = ((IModelActivity) getActivity()).getModel();
		} catch (ClassCastException e) {
			throw new ClassCastException(getActivity().toString() + " must implement IModelActivity");
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		Log.v("ChatFragment", "ChatFragment started");
		
		model.refreshSimpleChat(this, TASK_SIMPLE_CHAT);
		getActivity().setProgressBarIndeterminateVisibility(true);
	}
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.chat_list, container, false);
	}

	@Override
	public void onModelFinished(int tag, JSONArray result) {
		getActivity().setProgressBarIndeterminateVisibility(false);
		
//		ArrayList<String> messages = new ArrayList<String>();
//		try {
//			JSONObject next;
//			int i = 0;
//			while ((next = (JSONObject) result.opt(i)) != null)
//			{
//				++i;
//				messages.add(next.getString("message"));
//			}
//		} catch (Exception e) {
//			Log.e("PullChat", e.toString());
//			e.printStackTrace();
//		}
		
		View view = getView();
		ListView chats = (ListView) view.findViewById(R.id.chat_list);
        ChatAdapter chatAdapter = new ChatAdapter(view.getContext(), R.layout.chat_item, JSONTranslator.translateJSONChatEntrys(result)); //, android.R.id.text
        chats.setAdapter(chatAdapter);
	}
	
	private class ChatAdapter extends ArrayAdapter<ChatEntry> {

		private List<ChatEntry> entries;

		public ChatAdapter(Context context, int textViewResourceId, List<ChatEntry> entries) {
			super(context, textViewResourceId, entries);
			this.entries = entries;
		}
		
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.chat_item, null);
            }
            ChatEntry o = entries.get(position);
            if (o != null) {
                TextView sender = (TextView) v.findViewById(R.id.msg_sender);
                TextView msg = (TextView) v.findViewById(R.id.msg);
                TextView time = (TextView) v.findViewById(R.id.time_sent);
                sender.setText("Sender: "+ o.senderID);
                msg.setText(o.message);
                time.setText("Sent: "+ o.timestamp);
            }
            return v;
		}
	}
}
