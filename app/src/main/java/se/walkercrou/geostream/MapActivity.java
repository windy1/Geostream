package se.walkercrou.geostream;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import se.walkercrou.geostream.util.LocationManager;

/**
 * Main activity of application. Displays a map around your current location and displays nearby
 * posts. Provides navigation to {@link CameraActivity} and {@link PostDetailActivity}s.
 *
 * TODO: Replace OnMyLocationChangeListener with LocationManager entirely
 */
public class MapActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationChangeListener, GoogleMap.OnCameraChangeListener, OnMapReadyCallback {
    /**
     * The minimum map zoom enforced by this app. The reasoning behind this is to only make posts
     * that are in the user's vicinity available as a design choice.
     */
    public static final int MIN_MAP_ZOOM = 17;

    // map stuff
    private GoogleMap map;
    // used for getting initial location, after that the map is used
    private LocationManager locationManager;
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

        // get initial location
        locationManager = new LocationManager();
        locationManager.connect(this, this::onLocationEstablished);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;

        // get location
        Location location = locationManager.getLastLocation();
        if (location == null) {
            // could not get location
            E.location(this, (dialog, which) ->
                    locationManager.connect(this, this::onLocationEstablished)).show();
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

        // setup location updates
        // TODO: Permission check
        map.setMyLocationEnabled(true);
        map.setOnMyLocationChangeListener(this);

        centerMapOnLocation(location);
        refresh(); // place posts on map

        G.i("Map initialized.");
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        refresh(); // refresh map to reflect any changes
        if (!posts.containsKey(marker)) {
            Toast.makeText(this, R.string.error_post_no_longer_exists, Toast.LENGTH_SHORT).show();
            return true;
        }
        openPost(marker);
        return true;
    }

    @Override
    public void onMyLocationChange(Location location) {
        // keep map locked on position, don't relocate if zoomed in
        if (location != null && map.getCameraPosition().zoom == MIN_MAP_ZOOM)
            centerMapOnLocation(location);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // enforce the minimum zoom
        if (cameraPosition.zoom < MIN_MAP_ZOOM && map.getMyLocation() != null)
            centerMapOnLocation(map.getMyLocation());
    }

    /**
     * Show dialog while refreshing the posts on the map to reflect any new posts or deleted posts
     * on the server.
     */
    public void refresh() {
        progressDialog = ProgressDialog.show(this, getString(R.string.title_wait),
                getString(R.string.prompt_get_resource), true); // show dialog
        new Thread(this::_refresh).start(); // start refresh task in background
    }

    private void _refresh() {
        // get post list from server
        List<Post> newPosts;
        try {
            newPosts = Post.all(this, (error) -> E.internal(this, error));
        } catch (IOException e) {
            e.printStackTrace();
            E.connection(this, (d, w) -> refresh());
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

            if (!found) {
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
        centerMapOnLocation(map.getMyLocation());
        refresh();
    }

    public void openCamera(View view) {
        // called when the camera FAB is clicked
        startActivity(new Intent(this, CameraActivity.class));
    }

    // -----------------------------

    private void onLocationEstablished() {
        // called when the devices location is established for the first time
        G.i("Device location has been established.");
        setupMap();
        splashScreen.setVisibility(View.GONE);
        findViewById(R.id.fabs).setVisibility(View.VISIBLE); // make FAB container visible
        refreshBtn.show();
        cameraBtn.show();
    }

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
