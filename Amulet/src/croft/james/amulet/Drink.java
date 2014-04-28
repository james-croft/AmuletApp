package croft.james.amulet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class Drink {
	public String Name;
	public String Quantity;
	public String TimeStamp;

	private JSONArray currentDrinks;

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

	public void saveLocal(Context context, String filename) {
		JSONObject json = toJsonObject();

		currentDrinks = loadStateFromFileStream(context, filename + ".json");

		outputToFileStream(context, json, filename + ".json");
	}

	/**
	 * Gets the JSONArray of tasks from a drink's local store. 
	 *
	 * @param context - the current Activity context. 
	 * @param filename - the name of the file excluding file extension.
	 */
	public JSONArray loadLocal(Context context, String filename) {
		currentDrinks = loadStateFromFileStream(context, filename + ".json");
		return currentDrinks;
	}

	private JSONArray loadStateFromFileStream(Context context, String fileName) {
		FileInputStream inputStream;
		JSONArray jsonArray = null;

		try {
			inputStream = context.openFileInput(fileName);
			jsonArray = inputStreamToJSONObject(inputStream);

			inputStream.close();

		} catch (Exception e) {
			Log.e("log_tag", "Error in generating JSONObject " + e.toString());
		}

		if(jsonArray == null) {
			return new JSONArray();
		}

		return jsonArray;
	}

	private JSONArray inputStreamToJSONObject(FileInputStream inputStream) {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		String line;

		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			String jsonString = builder.toString();
			JSONArray jsonArray = new JSONArray(jsonString);

			return jsonArray;
		} catch (Exception e) {
			Log.e("log_tag", "Error in generating JSONObject " + e.toString());
		}

		return new JSONArray();
	}

	private void outputToFileStream(Context context, JSONObject jsonObject, String fileName) {
		FileOutputStream outputStream;

		try {
			outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			outputToFileStream(outputStream, context, jsonObject, fileName);
			outputStream.close();
		} catch (Exception e) {
			Log.e("log_tag", "Error in saving file " + e.toString());
		}
	}

	public void outputToFileStream(FileOutputStream outputFileStream, Context context, JSONObject jsonObject, String fileName) {

		if(currentDrinks == null) {
			currentDrinks = new JSONArray();
		}

		currentDrinks.put(jsonObject);

		try {
			outputFileStream.write(currentDrinks.toString().getBytes());
		} catch (Exception e) {
			Log.e("log_tag", "Error in saving file " + e.toString());
		}
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

	public void clearLocal(Context context, String filename) {
		try {
			context.deleteFile(filename + ".json");	
		} catch (Exception e) {
			Log.e("log_tag", "Error removing file " + e.toString());
		}
	}
}
