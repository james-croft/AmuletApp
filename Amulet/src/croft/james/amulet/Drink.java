package croft.james.amulet;

import java.text.SimpleDateFormat;

import org.json.JSONObject;

import android.util.Log;

public class Drink {
	public String Name;
	public String Quantity;
	public String TimeStamp;

	public Drink() {

	}

	/**
	 * Creates a new Drink object. 
	 *
	 * @param name - the name of the drink. 
	 * @param quantity - the number of drinks.
	 * @param timestamp - time the drinks were recorded.
	 */
	public Drink(String name, String quantity, String timestamp) {
		Name = name;
		Quantity = quantity;
		TimeStamp = timestamp;
	}

	public JSONObject toJsonObject() {
		JSONObject returnObj = new JSONObject();

		try {
			returnObj.put("timestamp", TimeStamp);
			returnObj.put("drinktype", Name);
			returnObj.put("unitsconsumed", Quantity);
		} catch(Exception e) {
			Log.e("log_tag", "Error in generating JSONObject " + e.toString());
		}

		return returnObj;
	}

	@Override
	public String toString() {
		String dateString  = "";
		
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy, HH:mm:ss");
			dateString= formatter.format(TimeStamp);
		} catch (Exception e) {
			Log.e("log_tag", "Error parsing TimeStamp " + e.toString());
			dateString = TimeStamp;
		}

		return String.format("%1$s - Quantity: %2$s - Recorded: %3$s", Name, Quantity, dateString);
	}

	public String toJsonString() {
		JSONObject json = toJsonObject();
		return json.toString();
	}
}
