package de.stadtrallye.rallyesoft;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import de.stadtrallye.rallyesoft.model.IMapListener;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.MapNode;

public class GameMapActivity extends SherlockMapActivity implements IMapListener {
	
	private MapView map;
	private Model model;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		setTitle(R.string.title_maps);
		setContentView(R.layout.map);
//		setBehindContentView(R.layout.dashboard_main);
		
		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);

//		ActionBar.Tab tab = getSupportActionBar().newTab();
		
//		SlidingMenu sm = getSlidingMenu();
//		sm.setEnabled(false);
//		sm.setShadowWidthRes(R.dimen.shadow_width);
//		sm.setShadowDrawable(R.drawable.shadow);
//		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
//		sm.setBehindCanvasTransformer(new CanvasTransformer() {
//			public void transformCanvas(Canvas canvas, float percentOpen) {
//				float scale = (float) (percentOpen*0.25 + 0.75);
//				canvas.scale(scale, scale, canvas.getWidth()/2, canvas.getHeight()/2);
//			}
//		});
        
//        ListView dashboard = (ListView) sm.findViewById(R.id.dashboard_list);
//        ArrayAdapter<String> dashAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, getResources().getStringArray(R.array.dashboard_entries));
//        dashboard.setAdapter(dashAdapter);
//        dashboard.setOnItemClickListener(this);
		
//		Context context = ab.getThemedContext();
//		ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.tabs, R.layout.sherlock_spinner_item);
//        list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		
		
//		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
//        ab.setListNavigationCallbacks(list, this);
//		ab.setSelectedNavigationItem(0);
		
		map = (MapView) findViewById(R.id.mapview);
		
		model = Model.getInstance(getApplicationContext(), true);
		model.setMapListener(this);
		model.getMapNodes();
	}
	
//	@Override
//	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
//		onSwitchTab(pos, id, TabEventSource.SlidingMenu);
//		getSlidingMenu().showAbove();
//		getSupportActionBar().setSelectedNavigationItem(pos);
//	}

//	@Override
//	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
//		return onSwitchTab(itemPosition, itemId, TabEventSource.NavList);
//	}
	
//	private enum TabEventSource { SlidingMenu, NavList };
	
//	private boolean onSwitchTab(int pos, long id, TabEventSource source) {
//		Fragment newFragment = null;
//		switch (pos) {
//		case 0: 
//			newFragment = new OverviewFragment();
//		break;
//		case 1:
//			newFragment = new MapFragment();
//		break;
//		case 3:
//			newFragment = new ChatFragment();
//			break;
//		default:
//			Toast.makeText(getApplicationContext(), getResources().getString(R.string.unsupported_link), Toast.LENGTH_SHORT).show();
//		return false;
//		}
//		
//		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//		ft.replace(R.id.content_frame, newFragment);
//		ft.commit();
//		currentFragment = newFragment;
//		
//		return true;
//	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void displayOverlay(List<MapNode> nodes) {
		
	}
	
	
	private class RallyeOverlay extends ItemizedOverlay<OverlayItem> {

		private Context context;
		private List<OverlayItem> nodes;
		
		public RallyeOverlay(Drawable defaultMarker, Context context, List<MapNode> nodes) {
			super(boundCenterBottom(defaultMarker));
			this.context = context;
			
			this.nodes = new ArrayList<OverlayItem>();
			
			for (MapNode node: nodes) {
				this.nodes.add(new OverlayItem(new GeoPoint(node.lat, node.lon), node.name, node.description));
				Log.d("GameMapActivity", node.toString());
			}
			
			
			populate();
			Toast.makeText(getApplicationContext(), nodes.size()+" Nodes loaded!", Toast.LENGTH_SHORT).show();
			
		}

		@Override
		protected OverlayItem createItem(int i) {
			return nodes.get(i);
		}

		@Override
		public int size() {
			return nodes.size();
		}
		
		@Override
		protected boolean onTap(int index) {
		  OverlayItem item = nodes.get(index);
		  AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		  dialog.setTitle(item.getTitle());
		  dialog.setMessage(item.getSnippet());
		  dialog.show();
		  return true;
		}
		
	}

	@Override
	public void nodeUpdate(List<MapNode> nodes) {
		ItemizedOverlay<OverlayItem> overlay = new RallyeOverlay(this.getResources().getDrawable(R.drawable.marker), this, nodes);
		map.getOverlays().add(overlay);
		map.invalidate();
	}

}
