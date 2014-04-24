package croft.james.amulet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class SharedPreferencesWrapper {

	public static SharedPreferences getPrefs(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static void savePref(Context context, String key, String value) {
		getPrefs(context).edit().putString(key, value).commit();
	}

	public static String getPref(Context context, String key, String defValue) {
		try {
			return getPrefs(context).getString(key, defValue);
		} catch (Exception e) {
			Log.e("log_tag", e.toString());
			return defValue;
		}
	}

	public static void removePref(Context context, String key) {
		getPrefs(context).edit().remove(key).commit();
	}
}
