package croft.james.amulet;

import org.json.JSONObject;

import android.util.Log;

public class InfoDrink {
	public String Name;
	public String Description;
	public int Quantity;
	public float Percent;
	
	public InfoDrink(){}
	
	public InfoDrink(String name, String description, int quantity, float percent) {
		Name = name;
		Description = description;
		Quantity = quantity;
		Percent = percent;
	}
	
	public String toString(){
		return Name + " - " + Description;
	}
	
	public JSONObject toJSONObject() {
		JSONObject returnObj = new JSONObject();
		
		try {
			returnObj.put("name", Name);
			returnObj.put("description", Description);
			returnObj.put("quantity", String.valueOf(Quantity));
			returnObj.put("percent", String.valueOf(Percent));
		} catch (Exception e) {
			Log.e("log_tag", "Error in generating JSONObject " + e.toString());
		}
		
		return returnObj;
	}
	
	public String toJSONString() {
		return toJSONObject().toString();
	}
}
