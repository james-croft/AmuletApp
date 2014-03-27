package croft.james.amulet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnRetrieveDataCompleted {	
	View _currentView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
			    View loginForm = findViewById(R.id.login_form);
			    _currentView = loginForm;
				showForm(loginForm, true);
			}
		});
		
		findViewById(R.id.register_button).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				View registerForm = findViewById(R.id.register_form);
				_currentView = registerForm;
				showForm(registerForm, true);
			}
		});
	}
	
	@Override 
	public void onBackPressed(){
		if(_currentView != null){
			showForm(_currentView, false);
			_currentView = null;
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onTaskCompleted(String responseData) {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	
	public void showForm(View v, final boolean show){
		View selectionOptions = findViewById(R.id.login_selection);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			v.setVisibility(View.VISIBLE);
			v.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0);

			selectionOptions.setVisibility(View.VISIBLE);
			selectionOptions.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1);
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show and hide the relevant UI components.
			v.setVisibility(show ? View.VISIBLE : View.GONE);
			selectionOptions.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
	private void loginUser(){
		Toast.makeText(this, getString(R.string.login_progress_signing_in), Toast.LENGTH_LONG).show();
		RetrieveHTTPDataAsync loginData = new RetrieveHTTPDataAsync(this);
		loginData.execute(getString(R.string.web_service_url) + getString(R.string.register_user));
	}
}
