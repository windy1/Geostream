package se.walkercrou.geostream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import se.walkercrou.geostream.util.App;

/**
 * Starting activity to check if the user is logged in and forward them to the proper activity.
 */
public class EntryActivity extends Activity {
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        App.init(this);
        SharedPreferences prefs = App.getSharedPreferences(this);
        if (prefs.getString("username", null) == null || prefs.getString("password", null) == null)
            startActivity(new Intent(this, LoginActivity.class));
        else
            startActivity(new Intent(this, MapsActivity.class));
    }
}
