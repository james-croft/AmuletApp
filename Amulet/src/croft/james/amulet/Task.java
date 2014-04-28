package croft.james.amulet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class Task {
	public String TaskType;
	public String TimeStamp;
	public String Result;
	public String Units;

	private JSONArray currentLocalTasks;

	public JSONObject toJsonObject() {
		JSONObject returnObj = new JSONObject();

		try {
			returnObj.put("tasktype", TaskType);
			returnObj.put("timestamp", TimeStamp);
			returnObj.put("taskvalue", Result);
			returnObj.put("unitsconsumed", Units);
		} catch(Exception e) {
			Log.e("log_tag", "Error in generating JSONObject " + e.toString());
		}

		return returnObj;
	}

	public void saveLocal(Context context, String filename) {
		JSONObject json = toJsonObject();

		currentLocalTasks = loadStateFromFileStream(context, filename + ".json");

		outputToFileStream(context, json, filename + ".json");
	}

	/**
	 * Gets the JSONArray of tasks from a tasktype's local store. 
	 *
	 * @param context - the current Activity context. 
	 * @param filename - the name of the file excluding file extension.
	 */
	public JSONArray loadLocal(Context context, String filename) {
		currentLocalTasks = loadStateFromFileStream(context, filename + ".json");
		return currentLocalTasks;
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

		if(currentLocalTasks == null) {
			currentLocalTasks = new JSONArray();
		}
		
		currentLocalTasks.put(jsonObject);

		try {
			outputFileStream.write(currentLocalTasks.toString().getBytes());
		} catch (Exception e) {
			Log.e("log_tag", "Error in saving file " + e.toString());
		}
	}

	public String toString() {
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
