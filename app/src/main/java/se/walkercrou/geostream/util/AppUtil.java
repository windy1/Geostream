package se.walkercrou.geostream.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import se.walkercrou.geostream.R;

/**
 * Helper class for convenience static functions related to application information
 */
public final class AppUtil {
    private static AppUtil instance;
    private String name;
    private String serverUrl;

    private AppUtil(Context context) {
        name = context.getString(R.string.app_name);
        serverUrl = context.getString(R.string.server_url);
    }

    /**
     * Initializes the singleton App object
     *
     * @param context of application
     */
    public static void init(Context context) {
        if (instance == null)
            instance = new AppUtil(context);
    }

    /**
     * Returns the application's name.
     *
     * @return application name
     */
    public static String getName() {
        return instance.name;
    }

    /**
     * Returns the server URL to use in networking.
     *
     * @return server url
     */
    public static String getServerUrl() {
        return instance.serverUrl;
    }

    /**
     * Logs a debug message.
     *
     * @param msg to log
     */
    public static void d(Object msg) {
        Log.d(getName(), msg.toString());
    }

    /**
     * Formats and logs a debug message.
     *
     * @param msg    to log
     * @param params for formatting
     */
    public static void d(Object msg, Object... params) {
        Log.d(getName(), String.format(msg.toString(), params));
    }

    /**
     * Logs an error message;
     *
     * @param msg to log
     * @param t   source of error
     */
    public static void e(String msg, Throwable t) {
        Log.e(getName(), msg, t);
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
