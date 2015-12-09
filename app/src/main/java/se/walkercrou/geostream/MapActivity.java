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
import java.util.List;
import java.util.Map;

import se.walkercrou.geostream.camera.CameraActivity;
import se.walkercrou.geostream.post.Post;
import se.walkercrou.geostream.post.PostDetailActivity;
import se.walkercrou.geostream.util.Dialogs;
import se.walkercrou.geostream.util.G;
import se.walkercrou.geostream.util.LocationManager;

/**
 * Main activity of application. Displays a map around your current location and displays nearby
 * posts.
 */
public class MapActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationChangeListener, GoogleMap.OnCameraChangeListener {

    public static final int MAP_ZOOM = 17;

    private GoogleMap map;
    private FloatingActionButton cameraBtn;
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

        // show splash screen if not yet shown
        // TODO: Show loading spinner while map is loading if splash screen is not displayed
        splashScreen = findViewById(R.id.splash);
        if (!G.app.splashed) {
            splashScreen.setVisibility(View.VISIBLE);
            G.app.splashed = true;
        }

        // setup up camera button
        cameraBtn = (FloatingActionButton) findViewById(R.id.fab_camera);
        cameraBtn.hide(false);

        // get initial location
        locationManager = new LocationManager(this);
        locationManager.connect(this::onLocationEstablished);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // marker on map clicked, open the post
        openPost(marker);
        return true;
    }

    @Override
    public void onMyLocationChange(Location location) {
        // keep map locked on position
        if (location != null)
            centerMapOnLocation(location);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // enforce the minimum zoom
        if (cameraPosition.zoom < MAP_ZOOM && map.getMyLocation() != null)
            centerMapOnLocation(map.getMyLocation());
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
        cameraBtn.show();
    }

    private void setupMap() {
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
                new LatLng(location.getLatitude(), location.getLongitude()), MAP_ZOOM
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
                Dialogs.connectionError(this, (dialog, which) -> {
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
