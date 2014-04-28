package croft.james.amulet;

import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnRetrieveDataCompleted {

	private DrawerLayout _drawerLayout;
	private ListView _drawerList;
	private ActionBarDrawerToggle _drawerToggle;
	private CharSequence _title;
	private CharSequence _drawerTitle;
	private String[] _menuItems;
	private Fragment fragment = null;
	private Drink drinkRecord = new Drink();

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

	private void recordDrink() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.drink_prompt_title);
		alert.setMessage(R.string.drink_prompt_message);

		final View drinkDialog = View.inflate(this, R.layout.dialog_drink, null);

		final MainActivity activity = this;

		alert.setView(drinkDialog);

		alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				EditText drinkNameText = (EditText)drinkDialog.findViewById(R.id.drink_name);
				String drinkName = drinkNameText.getText().toString();

				EditText drinkQuantityText = (EditText)drinkDialog.findViewById(R.id.drink_quantity);
				String drinkQuantity = drinkQuantityText.getText().toString();

				drinkRecord = new Drink();
				drinkRecord.Name = drinkName;
				drinkRecord.Quantity = drinkQuantity;

				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
				String dateString = formatter.format(new java.util.Date());

				drinkRecord.TimeStamp = dateString;

				JSONObject sendObject = new JSONObject();
				JSONArray drinkArray = drinkRecord.loadLocal(activity, "drink");			
				drinkArray.put(drinkRecord.toJsonObject());

				String username = SharedPreferencesWrapper.getPref(activity, "Username", "");
				String password = SharedPreferencesWrapper.getPref(activity, "Password", "");

				try {
					sendObject.put("username", username);
					sendObject.put("password", password);
					sendObject.put("entries", drinkArray);
				} catch (Exception ex) {
					Log.e("log_tag", "Error in JSONObject generation " + ex.toString());
				}

				// Send or store									
				SendHTTPDataAsync loginData = new SendHTTPDataAsync(activity, activity);
				loginData.execute(getString(R.string.web_service_url) + getString(R.string.drink), sendObject.toString());
				_drawerLayout.closeDrawer(_drawerList);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				_drawerLayout.closeDrawer(_drawerList);
			}
		});

		alert.show();
	}

	private void selectItem(int pos){
		boolean loadFragment = false;
		switch(pos) {
		case 0:
			fragment = new HomeFragment();
			loadFragment = true;			
			break;
		case 1:
			recordDrink();
			break;
		case 2:
			fragment = new DrinkDiaryFragment();
			loadFragment = true;
			break;
		}

		if(loadFragment) {
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

	@Override
	public void onTaskCompleted(String response) {

		if(response == "") {
			drinkRecord.saveLocal(this, "drink");
			Toast.makeText(this, "Couldn't send to server", Toast.LENGTH_LONG).show();
			Toast.makeText(this, "Stored locally", Toast.LENGTH_LONG).show();
		} else {
			drinkRecord.clearLocal(this, "drink");
			Toast.makeText(this, response, Toast.LENGTH_LONG).show();
			Toast.makeText(this, "Local drinks sent", Toast.LENGTH_LONG).show();
		}
	}

}
