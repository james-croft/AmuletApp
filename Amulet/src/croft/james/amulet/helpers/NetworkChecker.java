package croft.james.amulet.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChecker {
	public static int getNetworkStatus(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo network = cm.getActiveNetworkInfo();
		if(null != network) {
			if(network.getType() == ConnectivityManager.TYPE_WIFI)
				return 1;
			
			if(network.getType() == ConnectivityManager.TYPE_MOBILE)
				return 2;
		}
		
		return 0;
	}
	
	public static boolean getConnectivityStatus(Context context) {
		int status = getNetworkStatus(context);
		
		if(status == 1 || status == 2) { 
			return true;
		} else {
			return false;
		}
	}
}
