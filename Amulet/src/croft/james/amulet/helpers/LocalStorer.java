package croft.james.amulet.helpers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class LocalStorer {
	
	/**
	 * Saves a JSONObject to its JSONArray of objects in the local storage
	 *
	 * @param context - the current Activity context. 
	 * @param json - the JSON object to save.
	 * @param filename - the name of the file excluding file extension.
	 */
	public static void saveLocalJSONObject(Context context, JSONObject json, String filename) {
		JSONArray localContents = loadJSONFromFileStream(context, filename + ".json");
		localContents.put(json);
		
		outputJSONArrayToFileStream(context, localContents, filename + ".json");
	}
	
	/**
	 * Saves a JSONArray to the current JSONArray of objects in the local storage
	 *
	 * @param context - the current Activity context. 
	 * @param json - the JSON array to save.
	 * @param filename - the name of the file excluding file extension.
	 */
	public static void saveLocalJSONArray(Context context, JSONArray json, String filename) {
		JSONArray localContents = loadJSONFromFileStream(context, filename + ".json");
		
		for(int i = 0; i < json.length(); i++) {
			try {
				localContents.put(json.getJSONObject(i));
			} catch (JSONException e) {
				Log.e("log_tag", "Error getting JSONObject from JSONArray " + e.toString());
			}
		}
		
		outputJSONArrayToFileStream(context, localContents, filename + ".json");
	}
	
	/**
	 * Loads a JSONArray of JSONObjects from the local storage
	 *
	 * @param context - the current Activity context. 
	 * @param filename - the name of the file excluding file extension.
	 * @return returns a JSONArray of JSONObjects from local store if exists, else returns empty JSONArray.
	 */
	public static JSONArray getLocalJSONArray(Context context, String filename) {
		return loadJSONFromFileStream(context, filename + ".json");
	}
	
	/**
	 * Deletes the JSON file stored in local storage
	 *
	 * @param context - the current Activity context. 
	 * @param filename - the name of the file excluding file extension.
	 */
	public static void clearLocalJSON(Context context, String filename) {
		try {
			context.deleteFile(filename + ".json");	
		} catch (Exception e) {
			Log.e("log_tag", "Error removing file " + e.toString());
		}
	}
	
	/**
	 * Writes the JSONArray to a file using a FileOutputStream
	 *
	 * @param context - the current Activity context. 
	 * @param jsonArray - the array of JSONObjects to save.
	 * @param filename - the name of the file including file extension.
	 */
	private static void outputJSONArrayToFileStream(Context context, JSONArray jsonArray, String filename) {
		FileOutputStream outputStream;
		
		try {
			outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
			outputStream.write(jsonArray.toString().getBytes());
			outputStream.close();
		} catch (Exception e) {
			Log.e("log_tag", "Error in saving file " + e.toString());
		}
	}
	
	/**
	 * Reads the JSONArray from a file using a FileInputStream
	 *
	 * @param context - the current Activity context. 
	 * @param filename - the name of the file including file extension.
	 * @return returns a JSONArray of JSONObjects from local store if exists, else returns empty JSONArray.
	 */
	private static JSONArray loadJSONFromFileStream(Context context, String filename) {
		FileInputStream inputStream;
		JSONArray jsonArray = null;
		
		try {
			inputStream = context.openFileInput(filename);
			jsonArray = inputStreamToJSONArray(inputStream);
			
			inputStream.close();
		} catch (Exception e) {
			Log.e("log_tag", "Error in generating JSONObject " + e.toString());
			jsonArray = new JSONArray();
		}
		
		return jsonArray;
	}
	
	private static JSONArray inputStreamToJSONArray(FileInputStream inputStream) {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
		String line;
		JSONArray jsonArray;
		
		try {
			while((line = reader.readLine()) != null) {
				builder.append(line);
			}
			
			String jsonString = builder.toString();
			
			jsonArray = new JSONArray(jsonString);
		} catch (Exception e) {
			Log.e("log_tag", "Error in generating JSONObject " + e.toString());
			jsonArray = new JSONArray();
		}
		
		return jsonArray;
	}
}
