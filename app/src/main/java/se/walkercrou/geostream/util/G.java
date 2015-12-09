package se.walkercrou.geostream.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import se.walkercrou.geostream.R;

/**
 * Helper class for convenience static functions related to application information
 */
public final class G {
    // Application name
    public final String name;
    // Location of application server
    public final String serverUrl;
    // Collection of "client_secrets" for Post manipulation
    public final SharedPreferences secrets;
    // True if the splash screen has been displayed before
    public boolean splashed;

    public static G app;

    private G(Context context) {
        name = context.getString(R.string.app_name);
        serverUrl = context.getString(R.string.server_url);
        secrets = context.getSharedPreferences("ClientSecrets", Context.MODE_PRIVATE);
        splashed = false;
    }

    /**
     * Initializes the singleton App object
     *
     * @param context of application
     */
    public static void init(Context context) {
        if (app == null)
            app = new G(context);
    }

    /**
     * Logs a debug message.
     *
     * @param msg to log
     */
    public static void d(Object msg) {
        Log.d(app.name, msg.toString());
    }

    /**
     * Formats and logs a debug message.
     *
     * @param msg    to log
     * @param params for formatting
     */
    public static void d(Object msg, Object... params) {
        Log.d(app.name, String.format(msg.toString(), params));
    }

    /**
     * Logs an error message.
     *
     * @param msg to log
     * @param t   source of error
     */
    public static void e(String msg, Throwable t) {
        Log.e(app.name, msg, t);
    }

    /**
     * Logs an error message.
     *
     * @param msg to log
     */
    public static void e(String msg) {
        Log.e(app.name, msg);
    }

    /**
     * Returns true if the app is connected to the network.
     *
     * @param c context
     * @return true if connected
     */
    public static boolean isConnectedToNetwork(Context c) {
        ConnectivityManager cm
                = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }
}
