package se.walkercrou.geostream.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.net.HttpURLConnection;
import java.net.URL;

import se.walkercrou.geostream.R;

import static com.google.android.gms.common.api.GoogleApiClient.*;

/**
 * Helper class for convenience static functions
 */
public final class App {
    public static final String SERVER_URI = "http://192.168.128.236:8000";

    private static App instance;
    private String name;

    private App(Context context) {
        name = context.getString(R.string.app_name);
    }

    /**
     * Initializes the singleton App object
     *
     * @param context of application
     */
    public static void init(Context context) {
        if (instance == null)
            instance = new App(context);
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
     * Returns the application's preferences object.
     *
     * @param c context
     * @return preferences
     */
    public static SharedPreferences getSharedPreferences(Context c) {
        return c.getSharedPreferences(getName() + "Preferences", Context.MODE_PRIVATE);
    }

    /**
     * Logs a debug message.
     *
     * @param msg to log
     */
    public static void d(String msg) {
        Log.d(getName(), msg);
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
