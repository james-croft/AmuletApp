package croft.james.amulet;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnRetrieveDataCompleted {	
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
	}

	@Override
	public void onTaskCompleted(String responseData) {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	
	public void onBtnClicked(View v){
		if(v.getId() == R.id.sign_in_button)
			loginUser();
	}
	
	private void loginUser(){
		Toast.makeText(this, getString(R.string.login_progress_signing_in), Toast.LENGTH_LONG).show();
		RetrieveHTTPDataAsync loginData = new RetrieveHTTPDataAsync(this);
		loginData.execute(getString(R.string.web_service_url) + getString(R.string.register_user));
	}
	
	private void registerUser(){
		
	}
}
