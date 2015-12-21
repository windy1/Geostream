package se.walkercrou.geostream;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.melnykov.fab.FloatingActionButton;

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
 * posts.
 */
public class MapActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationChangeListener, GoogleMap.OnCameraChangeListener {

    /**
     * The minimum map zoom enforced by this app. The reasoning behind this is to only make posts
     * that are in the user's vicinity available as a design choice.
     */
    public static final int MIN_MAP_ZOOM = 17;

    private GoogleMap map;
    private FloatingActionButton cameraBtn, refreshBtn;
    // used for getting initial location, after that the map is used
    private LocationManager locationManager;
    private View splashScreen;
    private final Map<Marker, Post> posts = new HashMap<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        // hide action bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_map);

        // initialize singleton utility class
        G.init(this);

        // setup up camera button
        cameraBtn = (FloatingActionButton) findViewById(R.id.fab_camera);
        cameraBtn.hide(false);

        refreshBtn = (FloatingActionButton) findViewById(R.id.fab_refresh);
        refreshBtn.hide(false);

        // show splash screen if not yet shown
        // TODO: Show loading spinner while map is loading if splash screen is not displayed
        splashScreen = findViewById(R.id.splash);
        if (!G.app.splashed) {
            splashScreen.setVisibility(View.VISIBLE);
            G.app.splashed = true;
        }

        // get initial location
        locationManager = new LocationManager(this);
        locationManager.connect(this::onLocationEstablished);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // marker on map clicked, open the post
        refresh();
        if (!posts.containsKey(marker)) {
            Toast.makeText(this, R.string.error_post_no_longer_exists, Toast.LENGTH_SHORT).show();
            return true;
        }
        openPost(marker);
        return true;
    }

    @Override
    public void onMyLocationChange(Location location) {
        // keep map locked on position
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
     * Refreshes the posts on the map to reflect any new posts or deleted posts on the server.
     */
    public void refresh() {
        G.d("refreshing");
        List<Post> newPosts = getPosts();

        // remove posts that are no longer present
        Iterator<Map.Entry<Marker, Post>> iter = posts.entrySet().iterator();
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
                G.d("removing marker");
                marker.remove();
                iter.remove();
            }
        }

        // add new posts
        for (Post newPost : newPosts) {
            G.d("newPost = " + newPost.getId());
            boolean found = false;
            for (Post oldPost : posts.values()) {
                if (oldPost.getId() == newPost.getId())
                    found = true;
            }
            if (!found) {
                G.d("adding marker");
                placePost(newPost);
            }
        }
    }

    // -- Methods called from XML --

    public void refresh(View view) {
        // refreshes the posts on the map from the server
        centerMapOnLocation(map.getMyLocation());
        refresh();
    }

    public void openCamera(View view) {
        // called when the camera FAB is clicked, see respective layout file
        startActivity(new Intent(this, CameraActivity.class));
        // TODO: animation
    }

    private void onLocationEstablished() {
        // called when the devices location is established for the first time
        setupMap();
        splashScreen.setVisibility(View.GONE);
        findViewById(R.id.fabs).setVisibility(View.VISIBLE);
        refreshBtn.show();
        cameraBtn.show();
    }

    // -----------------------------

    private void setupMap() {
        // initialize map
        if (map == null) {
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (map != null) {
                // configure map settings
                UiSettings ui = map.getUiSettings();
                // lock map to position and do not allow zoom
                ui.setAllGesturesEnabled(false);
                ui.setZoomGesturesEnabled(true);
                ui.setMapToolbarEnabled(false);
                map.setOnCameraChangeListener(this);
                // redirect all marker clicks to this
                map.setOnMarkerClickListener(this);

                // setup location updates
                map.setMyLocationEnabled(true);
                map.setOnMyLocationChangeListener(this);

                centerMapOnLocation(locationManager.getLastLocation());

                // place posts on map
                List<Post> posts = getPosts();
                if (posts != null)
                    for (Post post : posts)
                        placePost(post);
            } else {
                // TODO: map could not be setup, show dialog
                throw new RuntimeException("map could not be setup");
            }
        }
    }

    private void centerMapOnLocation(Location location) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), MIN_MAP_ZOOM
        ));
    }

    private void placePost(Post post) {
        // create marker for post
        Location loc = post.getLocation();
        LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
        Marker marker = map.addMarker(new MarkerOptions().position(pos));
        posts.put(marker, post);
    }

    private void openPost(Marker marker) {
        // start the post detail activity
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST, posts.get(marker));
        startActivity(intent);
    }

    private List<Post> getPosts() {
        // TODO: only get posts in vicinity
        return Post.all((error) -> {
            if (error == null)
                // no connection
                E.connection(this, (dialog, which) -> {
                    // dismiss dialog and try again
                    dialog.dismiss();
                    getPosts();
                }).show();
            else
                // server responded with error, toast it
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }
}
