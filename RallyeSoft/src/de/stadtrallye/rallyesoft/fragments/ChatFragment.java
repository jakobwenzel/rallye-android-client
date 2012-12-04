package de.stadtrallye.rallyesoft.fragments;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

import de.stadtrallye.rallyesoft.IModelActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.async.PullChats;
import de.stadtrallye.rallyesoft.model.IModelResult;
import de.stadtrallye.rallyesoft.model.Model;

public class ChatFragment extends SherlockFragment implements IModelResult<JSONArray> {
	
	final static private int TASK_SIMPLE_CHAT = 101;
	
	private Model model;
	
	public ChatFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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
		
//		if (model != null)
			model.refreshSimpleChat(this, TASK_SIMPLE_CHAT);
	}
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.chat_list, container, false);
	}

	@Override
	public void onModelFinished(int tag, JSONArray result) {
		ArrayList<String> messages = new ArrayList<String>();
		try {
			JSONObject next;
			int i = 0;
			while ((next = (JSONObject) result.opt(i)) != null)
			{
				++i;
				messages.add(next.getString("message"));
			}
		} catch (Exception e) {
			Log.e("PullChat", e.toString());
			e.printStackTrace();
		}
		
		View view = getView();
		ListView chats = (ListView) view.findViewById(R.id.chat_list);
        ArrayAdapter<String> chatAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, messages);
        chats.setAdapter(chatAdapter);
	}
}
