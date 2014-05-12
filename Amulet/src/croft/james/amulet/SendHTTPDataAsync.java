package croft.james.amulet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import croft.james.amulet.helpers.NetworkChecker;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class SendHTTPDataAsync extends AsyncTask<String, Boolean, String> {

	private OnRetrieveDataCompleted _listener;
	private Activity _activity;
	private Context _context;

	public SendHTTPDataAsync(Activity currentActivity, OnRetrieveDataCompleted listener){
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
			response = postHttpData(urls[0], urls[1]);
		}
		return response;
	}

	private String postHttpData(String url, String jsonObject){
		StringBuilder builder = new StringBuilder();

		// If connected, get connected
		if(NetworkChecker.getConnectivityStatus(_context)){
			HttpClient client = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);

			StringEntity se = null;

			try{
				se = new StringEntity(jsonObject);
			} catch (Exception e) {
				Log.e("log_tag", "Error in creating string entity " + e.toString());
			}

			if(se != null) {
				httpPost.setEntity(se);

				httpPost.setHeader("Accept", "application/json");
				httpPost.setHeader("Content-type", "application/json");

				try {
					HttpResponse response = client.execute(httpPost);
					StatusLine statusLine = response.getStatusLine();

					int statusCode = statusLine.getStatusCode();

					if(statusCode == 200 || statusCode == 202) {
						HttpEntity entity = response.getEntity();
						InputStream content = entity.getContent();
						BufferedReader reader = new BufferedReader(new InputStreamReader(content));

						String line;
						while((line = reader.readLine()) != null) {
							builder.append(line);
						}
					} else {
						Log.e("log_tag", "Status code error " + statusLine.getReasonPhrase());
					}
				} catch(Exception e) {
					Log.e("log_tag", "Error in http connection "+e.toString());
				}
			}
		}

		return builder.toString();
	}

}
