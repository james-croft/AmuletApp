package croft.james.amulet;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnRetrieveDataCompleted {
	View _currentView;
	boolean _isRegistering = false;
	boolean _isLoggingIn = false;
	boolean _isResuming = false;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				View loginForm = findViewById(R.id.login_form);
				_currentView = loginForm;
				_isLoggingIn = true;
				showForm(loginForm, true);
			}
		});

		findViewById(R.id.register_button).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				View registerForm = findViewById(R.id.register_form);
				_currentView = registerForm;
				_isRegistering = true;
				showForm(registerForm, true);
			}
		});

		findViewById(R.id.login_user_button).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				loginUser(((EditText)findViewById(R.id.username)).getText().toString(), ((EditText)findViewById(R.id.password)).getText().toString());
			}
		});

		findViewById(R.id.register_user_button).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				registerUser();
			}
		});

		String username = SharedPreferencesWrapper.getPref(this, "Username", "");
		String password = SharedPreferencesWrapper.getPref(this, "Password", "");

		if(username != "" && password != "") {
			_isResuming = true;
			
			Toast.makeText(this, getString(R.string.login_progress_signing_in), Toast.LENGTH_LONG).show();
			
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
		}
	}

	@Override 
	public void onBackPressed(){
		if(_currentView != null){
			showForm(_currentView, false);
			_isRegistering = false;
			_isLoggingIn = false;
			_currentView = null;
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onTaskCompleted(String responseData) {

		JSONObject json = processJSON(responseData);

		String error = null;
		String result = null;
		try {
			error = json.getString("Error");
		} catch (JSONException e) {
			Log.e("log_tag", e.toString());

			try {
				if(_isRegistering){
					result = json.getString("Result");
				} else if (_isLoggingIn || _isResuming) {
					result = json.getString("FullName");
				}
			} catch (JSONException ex) {
				Log.e("log_tag", ex.toString());
			}
		}

		if(error != null) {
			Toast.makeText(this, error, Toast.LENGTH_LONG).show();
			Log.e("log_tag", error);
		} else {
			if(result != null) {
				if(_isRegistering) {
					SharedPreferencesWrapper.savePref(this, "Username", ((EditText)findViewById(R.id.email)).getText().toString());
					SharedPreferencesWrapper.savePref(this, "Password", ((EditText)findViewById(R.id.register_password)).getText().toString());
				} else if (_isLoggingIn) {
					SharedPreferencesWrapper.savePref(this, "Username", ((EditText)findViewById(R.id.username)).getText().toString());
					SharedPreferencesWrapper.savePref(this, "Password", ((EditText)findViewById(R.id.password)).getText().toString());
				}

				Intent intent = new Intent(this, MainActivity.class);
				startActivity(intent);
			} else {
				Toast.makeText(this, getString(R.string.server_error), Toast.LENGTH_LONG).show();
				Log.e("log_tag", getString(R.string.server_error));
			}
		}
	}

	private JSONObject processJSON(String jsonString) {

		JSONObject jsonObject;

		try {
			jsonObject = new JSONObject(jsonString);			
		} catch (JSONException e) {
			Log.e("log_tag", "Error in JSON processing " + e.toString());
			return null;
		}

		return jsonObject;
	}

	public void showForm(View v, final boolean show){
		View selectionOptions = findViewById(R.id.login_selection);

		int shortAnimTime = getResources().getInteger(
				android.R.integer.config_shortAnimTime);

		if (v.getVisibility() == View.VISIBLE) {
			v.setVisibility(View.INVISIBLE);
		} else {
			v.setVisibility(View.VISIBLE);
		}

		v.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0);

		if (v.getVisibility() == View.VISIBLE) {
			selectionOptions.setVisibility(View.INVISIBLE);
		} else {
			selectionOptions.setVisibility(View.VISIBLE);
		}

		selectionOptions.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1);

	}

	private void loginUser(String username, String password){
		Toast.makeText(this, getString(R.string.login_progress_signing_in), Toast.LENGTH_LONG).show();

		RetrieveHTTPDataAsync loginData = new RetrieveHTTPDataAsync(this, this);
		loginData.execute(getString(R.string.web_service_url) + getString(R.string.full_details) + String.format("username=%1$s&password=%2$s", username, password));
	}

	private void registerUser(){
		Toast.makeText(this, getString(R.string.register_progress_registering), Toast.LENGTH_LONG).show();

		String firstName = ((EditText)findViewById(R.id.first_name)).getText().toString();
		String surname = ((EditText)findViewById(R.id.surname)).getText().toString();
		String username = ((EditText)findViewById(R.id.email)).getText().toString();
		String password = ((EditText)findViewById(R.id.register_password)).getText().toString();

		RetrieveHTTPDataAsync loginData = new RetrieveHTTPDataAsync(this, this);
		loginData.execute(getString(R.string.web_service_url) + getString(R.string.register_user) + String.format("firstname=%1$s&Surname=%2$s&username=%3$s&password=%4$s", firstName, surname, username, password));
	}
}
