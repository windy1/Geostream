package se.walkercrou.geostream;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;

import java.util.List;

import se.walkercrou.geostream.camera.CameraActivity;
import se.walkercrou.geostream.net.Post;
import se.walkercrou.geostream.net.response.ApiResponse;

/**
 * Main activity of application. Displays a map around your current location and displays nearby
 * posts.
 */
public class MapActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerClickListener {

    public static final int MAP_ZOOM = 17;

    private GoogleMap map; // Might be null if Google Play services APK is not available.
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private List<Post> posts;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_maps);
        App.init(this);

        posts = getPosts();

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

    private List<Post> getPosts() {
        ApiResponse response = Post.listRequest().sendInBackground();
        String error = null;
        if (response == null) {
            // no connection
            error = getString(R.string.error_no_connection);
            error = String.format(error, App.getName());
        } else if (response.isError())
            // server returned error
            error = response.getErrorDetail();

        if (error != null)
            // display error
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();

        return response == null ? null : Post.parse((JSONArray) response.get());
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
        // configure map settings
        UiSettings ui = map.getUiSettings();
        ui.setAllGesturesEnabled(false);
        ui.setMapToolbarEnabled(false);
        map.setOnMarkerClickListener(this);

        // position on current location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), MAP_ZOOM
        ));

        if (posts != null) {
            for (Post post : posts)
                post.placeOnMap(map);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // start the post activity
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST, Post.getPostFor(marker));
        startActivity(intent);
        return true;
    }
}
