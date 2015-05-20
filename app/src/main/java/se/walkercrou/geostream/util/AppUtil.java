package se.walkercrou.geostream.util;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import se.walkercrou.geostream.R;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

/**
 * Helper class for convenience static functions
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
     * @param msg to log
     * @param params for formatting
     */
    public static void d(Object msg, Object... params) {
        Log.d(getName(), String.format(msg.toString(), params));
    }

    /**
     * Logs an error message;
     *
     * @param msg to log
     * @param t source of error
     */
    public static void e(String msg, Throwable t) {
        Log.e(getName(), msg, t);
    }

    /**
     * Returns a {@link GoogleApiClient} object for connecting to Google APIs.
     *
     * @param context of activity
     * @param callback1 for client to call
     * @param callback2 for client tp call
     * @return api client
     */
    public static GoogleApiClient buildGoogleApiClient(Context context,
                                                       ConnectionCallbacks callback1,
                                                       OnConnectionFailedListener callback2) {
        return new Builder(context)
                .addConnectionCallbacks(callback1)
                .addOnConnectionFailedListener(callback2)
                .addApi(LocationServices.API)
                .build();
    }
}
