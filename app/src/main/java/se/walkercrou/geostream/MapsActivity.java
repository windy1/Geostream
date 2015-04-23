package se.walkercrou.geostream;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

/**
 * Main activity of application. Displays a map around your current location and displays nearby
 * posts.
 */
public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final int MAP_ZOOM = 15;
    private GoogleMap map; // Might be null if Google Play services APK is not available.
    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_maps);

        // connect to location api
        googleApiClient = App.buildGoogleApiClient(this, this, this);
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        // connected to location api
        App.d("Connected to location API");
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        App.d(String.format("Last location : %s,%s",
                lastLocation.getLatitude(), lastLocation.getLongitude()));
        // setup map
        setUpMapIfNeeded();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    public void openCamera(View view) {
        // called when the camera FAB is clicked
        startActivity(new Intent(this, CameraActivity.class));
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        // position on current location
        map.getUiSettings().setScrollGesturesEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), MAP_ZOOM
        ));
    }
}
