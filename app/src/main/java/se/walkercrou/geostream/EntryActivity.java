package se.walkercrou.geostream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

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
        String username = prefs.getString("username", null);
        String password = prefs.getString("password", null);
        if (username == null || password == null) {
            App.d("User needs login, starting login activity");
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            App.d("User has logged in before, starting maps activity");
            startActivity(new Intent(this, MapsActivity.class));
        }
    }
}
