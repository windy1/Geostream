package se.walkercrou.geostream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import se.walkercrou.geostream.auth.LoginActivity;

/**
 * Starting activity to check if the user is logged in and forward them to the proper activity.
 */
public class EntryActivity extends Activity {
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        // initialize util class
        App.init(this);
        App.d("Started");

        // check if the user has logged in before and start the login activity if not
        // otherwise start the main map activity
        SharedPreferences prefs = App.getSharedPreferences(this);
        String username = prefs.getString(App.PREF_USER_NAME, null);
        String password = prefs.getString(App.PREF_USER_NAME, null);
        if (username == null || password == null || BuildConfig.DEBUG) {
            App.d("User needs login, starting login activity");
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            App.d("User has logged in before, starting maps activity");
            startActivity(new Intent(this, MapActivity.class));
        }
    }
}
