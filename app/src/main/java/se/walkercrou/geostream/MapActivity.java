package se.walkercrou.geostream;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
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
import se.walkercrou.geostream.util.AppUtil;
import se.walkercrou.geostream.util.DialogUtil;

/**
 * Main activity of application. Displays a map around your current location and displays nearby
 * posts.
 */
public class MapActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerClickListener {

    public static final int MAP_ZOOM = 17;

    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private List<Post> posts;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_map);
        AppUtil.init(this);

        // connect to location api, don't do anything else until we have the connection
        googleApiClient = AppUtil.buildGoogleApiClient(this, this, this);
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        // connected to location api
        AppUtil.d("Connected to location API");
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        AppUtil.d("Last location : %s,%s", lastLocation.getLatitude(), lastLocation.getLongitude());
        // setup map
        setUpMapIfNeeded();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // TODO: Handle lost connection
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO: Handle failed connection
    }

    public void openCamera(View view) {
        // called when the camera FAB is clicked, see respective layout file
        startActivity(new Intent(this, CameraActivity.class));
    }

    private List<Post> getPosts() {
        // get the json response from the server
        ApiResponse response = Post.listRequest().sendInBackground();
        if (response == null)
            // no connection
            DialogUtil.connectionError(this, (dialog, which) -> {
                // dismiss dialog and try again
                dialog.dismiss();
                posts = getPosts();
            }).show();
        else if (response.isError())
            // server responded with error
            Toast.makeText(this, response.getErrorDetail(), Toast.LENGTH_LONG).show();
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
        // lock map to position and do not allow zoom
        ui.setAllGesturesEnabled(false);
        ui.setMapToolbarEnabled(false);
        // redirect all marker clicks to this
        map.setOnMarkerClickListener(this);

        // position on current location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), MAP_ZOOM
        ));

        // retrieve the posts from the server and place them on the map
        posts = getPosts();
        if (posts != null) {
            for (Post post : posts)
                post.placeOnMap(map);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // start the post detail activity
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST, Post.getPostFor(marker));
        startActivity(intent);
        return true;
    }
}
