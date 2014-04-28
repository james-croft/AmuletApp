package croft.james.amulet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DrinkDiaryFragment extends Fragment implements OnRetrieveDataCompleted {
	Drink temp = new Drink();
	Vector<Drink> drinkArray = new Vector<Drink>();
	JSONArray drinks = new JSONArray();
	ListView listView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_drink_diary, container, false);

		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

		listView = (ListView)view.findViewById(R.id.drink_list);
		getServerDrinks();

		return view;
	}

	private void getServerDrinks() {
		Toast.makeText(getActivity(), "Getting drink diary...", Toast.LENGTH_LONG).show();

		String username = SharedPreferencesWrapper.getPref(getActivity(), "Username", "");
		String password = SharedPreferencesWrapper.getPref(getActivity(), "Password", "");

		RetrieveHTTPDataAsync loginData = new RetrieveHTTPDataAsync(getActivity(), this);
		loginData.execute(getString(R.string.web_service_url) + getString(R.string.drink_diary) + String.format("username=%1$s&password=%2$s", username, password));
	}

	@Override
	public void onTaskCompleted(String response) {
		if(response != "") {
			SharedPreferencesWrapper.savePref(getActivity(), "DrinkDiary", response);
			createListView(response);
		} else {
			String drinkDiary = SharedPreferencesWrapper.getPref(getActivity(), "DrinkDiary", "");

			if(drinkDiary != "") {
				createListView(drinkDiary);
			} else {
				Toast.makeText(getActivity(), "No Drink Diary Found!", Toast.LENGTH_LONG).show();
			}
		}

	}

	private void createListView (String jsonArrayString) {
		try {
			JSONArray array = new JSONArray(jsonArrayString);

			for(int i = 0; i < array.length(); i++ ) {
				try {
					JSONObject drinkObj = (JSONObject)array.get(i);
					
					Drink drink = new Drink(drinkObj.getString("drinktype"), drinkObj.getString("unitsconsumed"), drinkObj.getString("timestamp"));
					drinkArray.add(drink);
				} catch (JSONException e) {
					Log.e("log_tag", "Error in JSONObject generation " + e.toString());
				} catch (Exception e) {
					Log.e("log_tag", "Error thrown " + e.toString());
				}
			}

			DrinkDiaryAdapter adapter = new DrinkDiaryAdapter((Context)getActivity(), R.layout.adapter_drink_diary, drinkArray);

			try {
				listView.setAdapter(adapter);
			} catch (Exception e){
				Log.e("log_tag", "Error in ArrayAdapter creation " + e.toString());
			}

		} catch (JSONException e) {
			Log.e("log_tag", "Error in JSONObject generation " + e.toString());
		}
	}
	
	private class DrinkDiaryAdapter extends ArrayAdapter<Drink> {
		private Vector<Drink> drinkObjects;
		
		public DrinkDiaryAdapter(Context context, int viewId, Vector<Drink> objects) {
			super(context, viewId, objects);
			
			drinkObjects = objects;
		}
		
		public View getView(int position, View view, ViewGroup parent) {
			View v = view;
			
			if(v == null) {
				LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.adapter_drink_diary, null);
			}
			
			Drink i = drinkObjects.get(position);
			
			if(i != null) {
				TextView name = (TextView)v.findViewById(R.id.drink_name);
				TextView quantity = (TextView)v.findViewById(R.id.drink_quantity);
				TextView timestamp = (TextView)v.findViewById(R.id.drink_timestamp);
				
				if(name != null) {
					name.setText(String.format("%1$s", i.Name));
				}
				if(quantity != null) {
					quantity.setText(String.format("Quantity: %1$s", i.Quantity));
				}
				if(timestamp != null) {
					String dateString  = "";
					
					try {
						SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy, HH:mm:ss");
						dateString = formatter.format(i.TimeStamp.replace("T", " "));
					} catch (Exception e) {
						Log.e("log_tag", "Error parsing TimeStamp " + e.toString());
						dateString = i.TimeStamp;
					}
					
					timestamp.setText(String.format("Recorded: %1$s", dateString));
				}
			}
			
			return v;
		}
	}
}


