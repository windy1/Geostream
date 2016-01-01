package se.walkercrou.geostream.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import se.walkercrou.geostream.R;

/**
 * Helper class for convenience static functions related to application information
 */
public final class G {
    /**
     * Singleton object for accessing properties.
     */
    public static G app;
    /**
     * The preference file name that contains the "client_secrets" for the posts this device has
     * created.
     */
    public static final String CLIENT_SECRETS_FILE_NAME = "ClientSecrets";
    /**
     * The date format used by both the client and the server.
     */
    public static final DateFormat STANDARD_DATE_FORMAT
            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    /**
     * The time zone used by both the client and the server.
     */
    public static final TimeZone STANDARD_TIME_ZONE = TimeZone.getTimeZone("UTC");

    static {
        STANDARD_DATE_FORMAT.setTimeZone(STANDARD_TIME_ZONE);
    }

    public final String name; // application name
    public final String serverUrl; // location of server
    public final SharedPreferences secrets; // collection of client_secrets to edit posts
    public boolean splashed = false; // true if splash screen has been displayed

    private G(Context context) {
        name = context.getString(R.string.app_name);
        serverUrl = context.getString(R.string.server_url);
        secrets = context.getSharedPreferences("ClientSecrets", Context.MODE_PRIVATE);
    }

    /**
     * Initializes the singleton object
     *
     * @param context of application
     */
    public static void init(Context context) {
        if (app == null)
            app = new G(context);
    }

    /**
     * Logs an info message.
     *
     * @param msg to log
     */
    public static void i(Object msg) {
        Log.i(app.name, msg.toString());
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

    /**
     * Parses a server-formatted date string using the {@link #STANDARD_DATE_FORMAT}.
     *
     * @param str to parse
     * @return formatted date
     * @throws ParseException if the string is malformed
     */
    public static Date parseDateString(String str) throws ParseException {
        // e.g. "2015-12-16T19:11:46.202321Z"
        String[] dateTime = str.split("T"); // time and date separated by "T" char
        String date = dateTime[0];
        // we don't really care about the millis, cut off time from the dot
        String time = dateTime[1].substring(0, dateTime[1].indexOf('.'));
        // parse with standard date format, in UTC, like the server
        return STANDARD_DATE_FORMAT.parse(date + ' ' + time);
    }

    /**
     * Returns the string to display for timestamps on Posts or Comments. Compared to the current
     * date.
     *
     * @param date to get difference of
     * @return display string
     */
    public static String getTimeDisplay(Date date) {
        Date now = Calendar.getInstance(STANDARD_TIME_ZONE).getTime();
        long diff = now.getTime() - date.getTime();

        // display "<1m" if in the seconds
        long seconds = diff / 1000;
        if (seconds < 60)
            return "<1m";

        // display minutes if >1m but <1h
        long minutes = seconds / 60;
        if (minutes < 60)
            return minutes + "m";

        // display hours if >1h but <1d
        long hours = minutes / 60;
        if (hours < 24)
            return hours + "h";

        // otherwise display days
        return (hours / 24) + "d";
    }
}
