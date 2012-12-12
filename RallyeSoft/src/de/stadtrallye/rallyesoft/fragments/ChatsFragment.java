package de.stadtrallye.rallyesoft.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.viewpagerindicator.TitlePageIndicator;

import de.stadtrallye.rallyesoft.IModelActivity;
import de.stadtrallye.rallyesoft.R;
import de.stadtrallye.rallyesoft.model.Model;

/**
 * Tab that contains the chat functions (several chatrooms)
 * @author Ramon
 *
 */
public class ChatsFragment extends SherlockFragment {
	
	private Model model;
	private ViewPager pager;
	private TitlePageIndicator indicator;
	private ChatFragmentAdapter fragmentAdapter;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.chat_fragment, container, false);
		
		

        pager = (ViewPager)v.findViewById(R.id.pager);

        indicator = (TitlePageIndicator)v.findViewById(R.id.indicator);
		
		return v;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
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
		
		Log.v("ChatsFragment", "ChatFragment started");
		
		if (model.isLoggedIn())
		{
			fragmentAdapter = new ChatFragmentAdapter(getChildFragmentManager(), model.getChatRooms());
			pager.setAdapter(fragmentAdapter);
			indicator.setViewPager(pager);
		}
	}

	
}
