package se.walkercrou.geostream;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

/**
 * Helper class for convenience static functions
 */
public final class App {
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
     * Logs a debug message.
     *
     * @param msg to log
     */
    public static void d(Object msg) {
        Log.d(getName(), msg.toString());
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
