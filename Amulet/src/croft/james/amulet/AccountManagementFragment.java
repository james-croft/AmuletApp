package croft.james.amulet;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class AccountManagementFragment extends Fragment implements
		OnRetrieveDataCompleted {
	AccountManagementFragment _defaultFragment;
	View _view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		_view = inflater.inflate(R.layout.fragment_account_management,
				container, false);

		_defaultFragment = this;

		getActivity().setRequestedOrientation(
				ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		getActivity().setTitle("Manage Account");

		Toast.makeText(getActivity(), "Getting account details...",
				Toast.LENGTH_LONG).show();

		String username = SharedPreferencesWrapper.getPref(getActivity(),
				"Username", "");
		String password = SharedPreferencesWrapper.getPref(getActivity(),
				"Password", "");

		RetrieveHTTPDataAsync loginData = new RetrieveHTTPDataAsync(
				getActivity(), this);
		loginData.execute(getString(R.string.web_service_url)
				+ getString(R.string.full_details)
				+ String.format("username=%1$s&password=%2$s", username,
						password));

		((TextView) _view.findViewById(R.id.username)).setText(username);

		_view.findViewById(R.id.save_new_password_btn).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						String username = SharedPreferencesWrapper.getPref(
								getActivity(), "Username", "");
						String password = SharedPreferencesWrapper.getPref(
								getActivity(), "Password", "");
						try {
							String newPassword = ((TextView) _view
									.findViewById(R.id.new_password)).getText()
									.toString();
							String verifyPassword = ((TextView) _view
									.findViewById(R.id.new_password_verify))
									.getText().toString();

							if (newPassword.compareTo(verifyPassword) == 0) {
								RetrieveHTTPDataAsync newPasswordData = new RetrieveHTTPDataAsync(
										getActivity(), _defaultFragment);
								newPasswordData.execute(getString(R.string.web_service_url)
										+ getString(R.string.change_password)
										+ String.format(
												"username=%1$s&oldpassword=%2$s&newpassword=%3$s",
												username, password, newPassword));

							} else {
								Toast.makeText(
										getActivity(),
										"New password does not match your verification password",
										Toast.LENGTH_LONG).show();
							}
						} catch (Exception e) {
							Log.e("log_tag",
									"Error in getting data " + e.toString());
						}
					}
				});

		return _view;
	}

	@Override
	public void onTaskCompleted(String response) {
		if (response.toLowerCase().contains("ok")) {
			String newPassword = ((TextView) _view
					.findViewById(R.id.new_password)).getText().toString();

			SharedPreferencesWrapper.savePref(getActivity(), "Password",
					newPassword);

			getFragmentManager().popBackStackImmediate();
		} else {
			try {
				JSONObject obj = new JSONObject(response);
				String name = obj.getString("FullName");
				
				((TextView) _view.findViewById(R.id.fullname)).setText(name);
			} catch (Exception e) {
				Log.e("log_tag",
						"Error in getting data " + e.toString());
			}
		}
	}
}
