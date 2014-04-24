package croft.james.amulet;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity {

	private DrawerLayout _drawerLayout;
	private ListView _drawerList;
	private ActionBarDrawerToggle _drawerToggle;
	private CharSequence _title;
	private CharSequence _drawerTitle;
	private String[] _menuItems;
	private Fragment fragment = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupDrawer(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean drawerOpen = _drawerLayout.isDrawerOpen(_drawerList);
		menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(_drawerToggle.onOptionsItemSelected(item)){
			return true;
		}

		switch(item.getItemId()){
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void setTitle(CharSequence title){
		_title = title;
		getActionBar().setTitle(_title);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState){
		super.onPostCreate(savedInstanceState);
		_drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		_drawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * Sets up the Android Navigation Drawer for the application.
	 * This method will gather menu items from a string array and
	 * will associate them with the drawer. When this is done it
	 * assigns the drawer to the ActionBar of the current layout with
	 * event handlers for opening and closing the drawer.
	 * 
	 * @param savedInstanceState the bundle that is passed through onCreate override method
	 */
	private void setupDrawer(Bundle savedInstanceState){
		_title = _drawerTitle = getTitle();
		_menuItems = getResources().getStringArray(R.array.menu_items);
		_drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
		_drawerList = (ListView)findViewById(R.id.left_drawer);

		// Create a shadow to overlay main content
		_drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START); 
		// Setup the drawer's items with the menu items gathered from the string array
		_drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, _menuItems));
		_drawerList.setOnItemClickListener(new DrawerItemClickListener());

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// Ties the ActionBar and the Navigation Drawer
		_drawerToggle = new ActionBarDrawerToggle(
				this,
				_drawerLayout,
				R.drawable.ic_drawer,
				R.string.drawer_open,
				R.string.drawer_close
				){
			public void onDrawerClosed(View view){
				getActionBar().setTitle(_title);
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView){
				getActionBar().setTitle(_drawerTitle);
				invalidateOptionsMenu();
			}
		};

		_drawerLayout.setDrawerListener(_drawerToggle);

		if(savedInstanceState == null){
			selectItem(0);
		}
	}

	public class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id){
			selectItem(position);
		}
	}

	private void selectItem(int pos){
		switch(pos) {
		case 0:
			fragment = new HomeFragment();
			break;
		}

		Bundle args = new Bundle();
		args.putInt("fragment_number", pos);
		fragment.setArguments(args);

		FragmentManager fragmentManager = getFragmentManager();
		
		if(pos != 0) {
			fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack("fragment_number").commit();
		} else {
			fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
		}

		_drawerList.setItemChecked(pos, true);
		setTitle(_menuItems[pos]);
		_drawerLayout.closeDrawer(_drawerList);
	}

}
