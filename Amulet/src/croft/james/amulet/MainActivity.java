package croft.james.amulet;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import croft.james.amulet.helpers.LocalStorer;
import croft.james.amulet.helpers.UnitConverter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.res.AssetManager;
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
import android.widget.Spinner;
import android.widget.Toast;

/**
 * The main Activity for the application, after login. Uses fragments to swap
 * out rather than changing activities.
 * 
 * @author James Croft
 */
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
		loadPresetDrinks();
	}

	JSONArray infoDrinks;

	/**
	 * Loads in the preset drinks from assets into local storage
	 */
	private void loadPresetDrinks() {
		infoDrinks = LocalStorer.getLocalJSONArray(this, "info_drinks");

		if (infoDrinks.length() == 0) { // loads in the drinks if they aren't
										// already stored in the local storage
			// load preset drinks
			AssetManager mgr = this.getAssets();
			InputStream drinksFile;
			byte[] array = new byte[0];
			try {
				drinksFile = mgr.open("Files/drinks.json");
				array = new byte[drinksFile.available()];
				drinksFile.read(array);
				drinksFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			JSONObject drinksObj = new JSONObject();
			JSONArray drinksArray = new JSONArray();

			try {
				drinksObj = new JSONObject(new String(array));
				drinksArray = drinksObj.getJSONArray("drinks");
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (drinksArray.length() > 0) {
				LocalStorer
						.saveLocalJSONArray(this, drinksArray, "info_drinks");
				infoDrinks = drinksArray;
			}
		}
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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (_drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		_title = title;
		getActionBar().setTitle(_title);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		_drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		_drawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * Sets up the Android Navigation Drawer for the application. This method
	 * will gather menu items from a string array and will associate them with
	 * the drawer. When this is done it assigns the drawer to the ActionBar of
	 * the current layout with event handlers for opening and closing the
	 * drawer.
	 * 
	 * @param savedInstanceState
	 *            the bundle that is passed through onCreate override method
	 */
	private void setupDrawer(Bundle savedInstanceState) {
		_title = _drawerTitle = getTitle();
		_menuItems = getResources().getStringArray(R.array.menu_items);
		_drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		_drawerList = (ListView) findViewById(R.id.left_drawer);

		// Create a shadow to overlay main content
		_drawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		// Setup the drawer's items with the menu items gathered from the string
		// array
		_drawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, _menuItems));
		_drawerList.setOnItemClickListener(new DrawerItemClickListener());

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// Ties the ActionBar and the Navigation Drawer
		_drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(_title);
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(_drawerTitle);
				invalidateOptionsMenu();
			}
		};

		_drawerLayout.setDrawerListener(_drawerToggle);

		if (savedInstanceState == null) {
			selectItem(0);
		}
	}

	public class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position);
		}
	}

	private void logout() {
		SharedPreferencesWrapper.savePref(this, "Username", null);
		SharedPreferencesWrapper.savePref(this, "Password", null);
		 finish();
	}

	Vector<InfoDrink> _infoDrinks = new Vector<InfoDrink>();

	private void addInfoDrinks(View v) {
		Spinner spinner = (Spinner) v.findViewById(R.id.drink_list_box);
		List<String> list = new ArrayList<String>();

		for (int i = 0; i < infoDrinks.length(); i++) {
			try {
				JSONObject obj = infoDrinks.getJSONObject(i);
				InfoDrink drink = new InfoDrink(obj.getString("name"),
						obj.getString("description"), obj.getInt("quantity"),
						obj.getLong("percent"));
				_infoDrinks.add(drink);
				list.add(drink.toString());
				Log.i("log", obj.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(dataAdapter);

	}

	/**
	 * Opens up an alert for recording drinks without having to perform a task
	 */
	private void recordDrink() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.drink_prompt_title);
		alert.setMessage(R.string.drink_prompt_message);

		final View drinkDialog = View.inflate(this, R.layout.dialog_drink, null);
		
		addInfoDrinks(drinkDialog);

		final MainActivity activity = this;

		alert.setView(drinkDialog);

		alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String drinkName = "";
				try{
					drinkName = ((EditText)drinkDialog.findViewById(R.id.drink_name)).getText().toString();
				}catch (Exception ex){
					Log.e("log_error", ex.getMessage());
				}
				
				if(drinkName.isEmpty()) {
					drinkName = ((Spinner)drinkDialog.findViewById(R.id.drink_list_box)).getSelectedItem().toString();
				}

				float drinkPercentage = -1;
				
				try{
					drinkPercentage = Float.parseFloat(((EditText)drinkDialog.findViewById(R.id.drink_percentage)).getText().toString());
				}catch (Exception ex){
					Log.e("log_error", ex.getMessage());
				}
				
				if(drinkPercentage == -1) {
					drinkPercentage = _infoDrinks.get((int) ((Spinner)drinkDialog.findViewById(R.id.drink_list_box)).getSelectedItemId()).Percent;
				}
				
				String units = "";
				
				float drinkUnits = 0;
				try{
					drinkUnits = Float.parseFloat(((EditText)drinkDialog.findViewById(R.id.drink_unit)).getText().toString());
				} catch (Exception ex) {
					Log.e("error", ex.getMessage());
				}
				
				if(drinkUnits == 0) {
					float drinkQuantity = 0;
					try{
						drinkQuantity = Float.parseFloat(((EditText)drinkDialog.findViewById(R.id.drink_quantity)).getText().toString());
					} catch (Exception ex) {
						Log.e("error", ex.getMessage());
					}
					
					units = String.valueOf(UnitConverter.ToUnit(drinkQuantity, drinkPercentage));
				}
				
				drinkRecord = new Drink();
				drinkRecord.Name = drinkName;
				drinkRecord.Quantity = units;

				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
				String dateString = formatter.format(new java.util.Date());

				drinkRecord.TimeStamp = dateString;

				JSONObject sendObject = new JSONObject();
				JSONArray drinkArray = LocalStorer.getLocalJSONArray(activity, "drink");			
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

	/**
	 * Processes the selected item of the Navigation Drawer
	 * 
	 * @param pos
	 *            - the position of the item selected in the drawer
	 */
	private void selectItem(int pos) {
		boolean loadFragment = false;
		switch (pos) {
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
		case 3:
			fragment = new TaskHistoryFragment();
			loadFragment = true;
			break;
		case 4:
			fragment = new AccountManagementFragment();
			loadFragment = true;
			break;
		case 5:
			fragment = new UnitConverterFragment();
			loadFragment = true;
			break;
		case 6:
			logout();
			break;
		}

		if (loadFragment) {
			Bundle args = new Bundle();
			args.putInt("fragment_number", pos);
			fragment.setArguments(args);

			FragmentManager fragmentManager = getFragmentManager();

			if (pos != 0) {
				fragmentManager.beginTransaction()
						.replace(R.id.content_frame, fragment)
						.addToBackStack("fragment_number").commit();
			} else {
				fragmentManager.beginTransaction()
						.replace(R.id.content_frame, fragment).commit();
			}

			_drawerList.setItemChecked(pos, true);
			setTitle(_menuItems[pos]);
			_drawerLayout.closeDrawer(_drawerList);
		}
	}

	@Override
	public void onTaskCompleted(String response) {

		if (response == "") {
			LocalStorer.saveLocalJSONObject(this, drinkRecord.toJsonObject(),
					"drink");
			Toast.makeText(this, "Couldn't send to server", Toast.LENGTH_LONG)
					.show();
			Toast.makeText(this, "Stored locally", Toast.LENGTH_LONG).show();
		} else {
			LocalStorer.clearLocalJSON(this, "drink");
			Toast.makeText(this, response, Toast.LENGTH_LONG).show();
			Toast.makeText(this, "Local drinks sent", Toast.LENGTH_LONG).show();
		}
	}

}
