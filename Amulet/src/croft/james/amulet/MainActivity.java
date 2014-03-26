package croft.james.amulet;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
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
    
    private void setupDrawer(Bundle savedInstanceState){
        _title = _drawerTitle = getTitle();
    	_menuItems = getResources().getStringArray(R.array.menu_items);
    	_drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
    	_drawerList = (ListView)findViewById(R.id.left_drawer);
    	
    	_drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    	_drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, _menuItems));
    	_drawerList.setOnItemClickListener(new DrawerItemClickListener());
    	
    	getActionBar().setDisplayHomeAsUpEnabled(true);
    	getActionBar().setHomeButtonEnabled(true);
    	
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
		// ToDo: Implement fragments
		
		_drawerList.setItemChecked(pos, true);
		setTitle(_menuItems[pos]);
		_drawerLayout.closeDrawer(_drawerList);
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
}
