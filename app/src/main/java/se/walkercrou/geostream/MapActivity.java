package se.walkercrou.geostream;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.melnykov.fab.FloatingActionButton;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import se.walkercrou.geostream.camera.CameraActivity;
import se.walkercrou.geostream.post.Post;
import se.walkercrou.geostream.post.PostDetailActivity;
import se.walkercrou.geostream.util.E;
import se.walkercrou.geostream.util.G;
import se.walkercrou.geostream.util.LocationApi;

import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import static com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import static com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;

/**
 * Main activity of application. Displays a map around your current location and displays nearby
 * posts. Provides navigation to {@link CameraActivity} and {@link PostDetailActivity}s.
 */
public class MapActivity extends FragmentActivity implements OnMarkerClickListener,
        OnCameraChangeListener, OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener,
        LocationListener {
    /**
     * The minimum map zoom enforced by this app. The reasoning behind this is to only make posts
     * that are in the user's vicinity available as a design choice.
     */
    public static final int MIN_MAP_ZOOM = 17;

    // map stuff
    private GoogleMap map;
    private LocationApi locationApi;
    private boolean lostLocation;
    private final Map<Marker, Post> posts = new HashMap<>(); // map the map markers to posts

    // ui stuff
    private FloatingActionButton cameraBtn, refreshBtn;
    private View splashScreen; // displayed when application is launched
    private final Handler handler = new Handler();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // hide action bar
        setContentView(R.layout.activity_map);
        G.init(this); // initialize singleton utility class

        // setup buttons
        cameraBtn = (FloatingActionButton) findViewById(R.id.fab_camera);
        cameraBtn.hide(false); // hide until splash screen is gone

        refreshBtn = (FloatingActionButton) findViewById(R.id.fab_refresh);
        refreshBtn.hide(false); // hide until splash screen is gone

        // show splash screen if not yet shown
        splashScreen = findViewById(R.id.splash);
        if (!G.app.splashed) {
            splashScreen.setVisibility(View.VISIBLE);
            G.app.splashed = true;
        }
    }

    @Override
    protected void onStart() {
        locationApi = new LocationApi(this);
        locationApi.connect(this, this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        locationApi.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        G.i("Connected to location services.");

        if (lostLocation) {
            Toast.makeText(this, R.string.prompt_location_restored, Toast.LENGTH_LONG).show();
            lostLocation = false;
        }

        setupMap();
        splashScreen.setVisibility(View.GONE);
        findViewById(R.id.fabs).setVisibility(View.VISIBLE); // make FAB container visible
        refreshBtn.show();
        cameraBtn.show();

        locationApi.startLocationUpdates(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        G.i("Connection to location services lost.");
        Toast.makeText(this, R.string.error_lost_connection, Toast.LENGTH_LONG).show();
        lostLocation = true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        G.i("Connection to location services failed.");
        E.location(this, (d, w) -> locationApi.connect(this, this)).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (map.getCameraPosition().zoom == MIN_MAP_ZOOM)
            centerMapOnLocation(location);
        refreshInBackground();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 862) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                G.i("Device has been granted location permissions.");
                locationApi.connect(this, this);
            } else {
                G.i("Device has been denied location permissions.");
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_error)
                        .setMessage(R.string.error_location_required)
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                            MapActivity.this.finish();
                        })
                        .show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;

        // get location
        Location location = locationApi.getLastLocation();
        if (location == null) {
            // could not get location
            E.location(this, (dialog, which) -> onMapReady(map)).show();
            return;
        }

        // configure map settings
        UiSettings ui = map.getUiSettings();
        // lock map to position and do not allow zoom
        ui.setAllGesturesEnabled(false);
        ui.setZoomGesturesEnabled(true);
        ui.setMapToolbarEnabled(false);
        map.setOnCameraChangeListener(this); // listen for camera changes
        map.setOnMarkerClickListener(this); // redirect all marker clicks to this
        map.setMyLocationEnabled(true);

        centerMapOnLocation(location);
        refreshWithDialog(); // place posts on map

        G.i("Map initialized.");
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        refreshWithDialog(); // refresh map to reflect any changes
        if (!posts.containsKey(marker)) {
            Toast.makeText(this, R.string.error_post_no_longer_exists, Toast.LENGTH_SHORT).show();
            return true;
        }
        openPost(marker);
        return true;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // enforce the minimum zoom
        Location location = locationApi.getLastLocation();
        if (cameraPosition.zoom < MIN_MAP_ZOOM && location != null)
            centerMapOnLocation(location);
    }

    /**
     * Show dialog while refreshing the posts on the map to reflect any new posts or deleted posts
     * on the server.
     */
    public void refreshWithDialog() {
        progressDialog = ProgressDialog.show(this, getString(R.string.title_wait),
                getString(R.string.prompt_get_resource), true); // show dialog
        refreshInBackground();
    }

    /**
     * Refreshes the map in a background thread.
     */
    public void refreshInBackground() {
        VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();
        new Thread(() -> refresh(visibleRegion)).start();
    }

    private void refresh(VisibleRegion visibleRegion) {

        // determine range of posts to retrieve
        LatLng topLeftCorner = visibleRegion.farLeft;
        LatLng bottomRightCorner = visibleRegion.nearRight;

        double fromLat = Math.min(topLeftCorner.latitude, bottomRightCorner.latitude);
        double toLat = Math.max(topLeftCorner.latitude, bottomRightCorner.latitude);
        double fromLng = Math.min(topLeftCorner.longitude, bottomRightCorner.longitude);
        double toLng = Math.max(topLeftCorner.longitude, bottomRightCorner.longitude);

        // get posts within visible range from server
        List<Post> newPosts;
        try {
            newPosts = Post.range(this, fromLat, toLat, fromLng, toLng, e -> E.internal(this, e));
        } catch (IOException e) {
            e.printStackTrace();
            handler.post(() -> E.connection(this, (d, w) -> refreshWithDialog()));
            return;
        }

        // post list should never be null at this point
        if (newPosts == null)
            throw new RuntimeException("null Post list with no error");

        // remove posts that are no longer present in list
        Iterator<Map.Entry<Marker, Post>> iter = posts.entrySet().iterator();
        int removed = 0;
        while (iter.hasNext()) {
            Map.Entry<Marker, Post> entry = iter.next();
            Marker marker = entry.getKey();
            Post oldPost = entry.getValue();

            // look for posts that are not in the newPosts list
            boolean found = false;
            for (Post newPost : newPosts) {
                if (oldPost.getId() == newPost.getId())
                    found = true;
            }

            if (!found) {
                handler.post(marker::remove); // post to main thread
                iter.remove();
                removed++;
            }
        }

        // add new posts
        int added = 0;
        for (Post newPost : newPosts) {
            boolean found = false;
            for (Post oldPost : posts.values()) {
                if (oldPost.getId() == newPost.getId())
                    found = true;
            }

            if (!found && !newPost.isHidden()) {
                handler.post(() -> placePost(newPost)); // post to main thread
                added++;
            }
        }

        if (progressDialog != null)
            progressDialog.dismiss();

        // print some info about task
        G.i("Map refreshed.");
        G.i("  Removed: " + removed);
        G.i("  Added: " + added);
    }

    // -- Methods called from XML --

    public void refresh(View view) {
        // called when the refresh FAB is clicked
        centerMapOnLocation(locationApi.getLastLocation());
        refreshWithDialog();
    }

    public void openCamera(View view) {
        // called when the camera FAB is clicked
        startActivity(new Intent(this, CameraActivity.class));
    }

    // -----------------------------

    private void setupMap() {
        FragmentManager fm = getSupportFragmentManager();
        ((SupportMapFragment) fm.findFragmentById(R.id.map)).getMapAsync(this);
    }

    private void centerMapOnLocation(Location location) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), MIN_MAP_ZOOM
        ));
    }

    private void placePost(Post post) {
        // place a marker on the map linked to the post
        Location loc = post.getLocation();
        LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
        Marker marker = map.addMarker(new MarkerOptions().position(pos));
        posts.put(marker, post);
        G.i("Post #" + post.getId() + " placed on map at " + pos);
    }

    private void openPost(Marker marker) {
        // start a post detail activity
        Post post = posts.get(marker);
        G.i("Starting PostDetailActivity for Post #" + post.getId() + '.');
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST, post);
        startActivity(intent);
    }
}
