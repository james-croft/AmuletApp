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

import android.os.AsyncTask;

public class RetrieveHTTPDataAsync extends AsyncTask<String, Boolean, String> {

	private OnRetrieveDataCompleted _listener;
	
	public RetrieveHTTPDataAsync(OnRetrieveDataCompleted listener){
		_listener = listener;
	}
	
	@Override
	protected void onPostExecute(String responseData){
		_listener.onTaskCompleted(responseData);
	}
	
	@Override
	protected String doInBackground(String... urls) {
		String responseData = new String();
		if(urls.length > 0){
			responseData = getHttpData(urls[0]);
		}	
		return responseData;
	}
	
	private String getHttpData(String url){
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		
		try{
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			
			int statusCode = statusLine.getStatusCode();
			if(statusCode == 200){
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				
				String line;
				while((line = reader.readLine()) != null){
					builder.append(line);
				}
			}
		} catch (Exception e){}
		
		return builder.toString();
	}
}
