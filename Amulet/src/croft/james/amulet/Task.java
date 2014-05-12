package croft.james.amulet;

import org.json.JSONObject;

import android.util.Log;

public class Task {
	public String TaskType;
	public String TimeStamp;
	public String Result;
	public String Units;
	
	public Task() {
		
	}
	
	public Task(String name, String quantity, String timestamp, String units) {
		TaskType = name;
		Result = quantity;
		TimeStamp = timestamp;
		Units = units;
	}

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

	public String toString() {
		JSONObject json = toJsonObject();
		return json.toString();
	}
}
