package croft.james.amulet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class RetrieveHTTPDataAsync extends AsyncTask<String, Boolean, String> {

	private OnRetrieveDataCompleted _listener;
	private Activity _activity;
	private Context _context;

	public RetrieveHTTPDataAsync(Activity currentActivity, OnRetrieveDataCompleted listener){
		_activity = currentActivity;
		_listener = listener;
		_context = _activity.getApplicationContext();
	}

	@Override
	protected void onPostExecute(String response){
		_listener.onTaskCompleted(response);
	}

	@Override
	protected String doInBackground(String... urls) {
		String response = new String();
		if(urls.length > 0){
			response = getHttpData(urls[0]);
		}
		return response;
	}

	private String getHttpData(String url){
		StringBuilder builder = new StringBuilder();

		// If connected, get connected
		if(NetworkChecker.getConnectivityStatus(_context)){
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);

			try {
				HttpResponse response = client.execute(httpGet);
				StatusLine statusLine = response.getStatusLine();

				int statusCode = statusLine.getStatusCode();

				if(statusCode == 200) {
					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(content));

					String line;
					while((line = reader.readLine()) != null) {
						builder.append(line);
					}
				}
			} catch(Exception e) {
				Log.e("log_tag", "Error in http connection "+e.toString());
			}
		}

		return builder.toString();
	}
}
