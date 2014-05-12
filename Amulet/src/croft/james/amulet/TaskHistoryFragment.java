package croft.james.amulet;

import java.text.SimpleDateFormat;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TaskHistoryFragment extends Fragment implements OnRetrieveDataCompleted {
	Task temp = new Task();
	Vector<Task> taskArray = new Vector<Task>();
	JSONArray tasks = new JSONArray();
	ListView listView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_task_history, container, false);

		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		
		getActivity().setTitle("Task History");
		
		listView = (ListView)view.findViewById(R.id.task_list_view);
		getServerTaskHistory("all");

	    return view;
	}
	
	private void getServerTaskHistory(String task) {
		Toast.makeText(getActivity(), "Getting task history...", Toast.LENGTH_LONG).show();

		String username = SharedPreferencesWrapper.getPref(getActivity(), "Username", "");
		String password = SharedPreferencesWrapper.getPref(getActivity(), "Password", "");

		RetrieveHTTPDataAsync loginData = new RetrieveHTTPDataAsync(getActivity(), this);
		loginData.execute(getString(R.string.web_service_url) + getString(R.string.task_history) + String.format("username=%1$s&password=%2$s&tasktype=%3$s", username, password, task));
	}
		
	@Override
	public void onTaskCompleted(String response) {
		if(response != "") {
			SharedPreferencesWrapper.savePref(getActivity(), "TaskHistory", response);
			createListView(response);
		} else {
			String taskHistory = SharedPreferencesWrapper.getPref(getActivity(), "TaskHistory", "");

			if(taskHistory != "") {
				createListView(taskHistory);
			} else {
				Toast.makeText(getActivity(), "No Task History Found!", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private void createListView (String jsonArrayString) {
		try {
			JSONArray array = new JSONArray(jsonArrayString);

			for(int i = 0; i < array.length(); i++ ) {
				try {
					JSONObject taskObj = (JSONObject)array.get(i);
					
					Task task = new Task(taskObj.getString("tasktype"), taskObj.getString("taskvalue"), taskObj.getString("timestamp"), taskObj.getString("unitsconsumed"));
					taskArray.add(task);
				} catch (JSONException e) {
					Log.e("log_tag", "Error in JSONObject generation " + e.toString());
				} catch (Exception e) {
					Log.e("log_tag", "Error thrown " + e.toString());
				}
			}

			TaskHistoryAdapter adapter = new TaskHistoryAdapter((Context)getActivity(), R.layout.adapter_task_history, taskArray);

			try {
				listView.setAdapter(adapter);
			} catch (Exception e){
				Log.e("log_tag", "Error in ArrayAdapter creation " + e.toString());
			}

		} catch (JSONException e) {
			Log.e("log_tag", "Error in JSONObject generation " + e.toString());
		}
	}
	
	private class TaskHistoryAdapter extends ArrayAdapter<Task> {
		private Vector<Task> taskObjects;
		
		public TaskHistoryAdapter(Context context, int viewId, Vector<Task> objects) {
			super(context, viewId, objects);
			
			taskObjects = objects;
		}
		
		public View getView(int position, View view, ViewGroup parent) {
			View v = view;
			
			if(v == null) {
				LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.adapter_task_history, null);
			}
			
			Task i = taskObjects.get(position);
			
			if(i != null) {
				TextView name = (TextView)v.findViewById(R.id.task_name);
				TextView quantity = (TextView)v.findViewById(R.id.task_score);
				TextView timestamp = (TextView)v.findViewById(R.id.task_timestamp);
				
				if(name != null) {
					name.setText(String.format("%1$s", i.TaskType));
				}
				if(quantity != null) {
					quantity.setText(String.format("Result: %1$s", i.Result));
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
