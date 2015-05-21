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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;

import java.util.List;

import se.walkercrou.geostream.camera.CameraActivity;
import se.walkercrou.geostream.net.response.ApiResponse;
import se.walkercrou.geostream.util.AppUtil;
import se.walkercrou.geostream.util.DialogUtil;

/**
 * Main activity of application. Displays a map around your current location and displays nearby
 * posts.
 */
public class MapActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener {

    public static final int MAP_ZOOM = 17;

    private LocationServices locationServices;
    private GoogleMap map;
    private List<Post> posts;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_map);
        AppUtil.init(this);

        // setup location services
        locationServices = new LocationServices(this);
        // setup map after connection
        locationServices.connect(this::setUpMapIfNeeded);
    }

    public void openCamera(View view) {
        // called when the camera FAB is clicked, see respective layout file
        startActivity(new Intent(this, CameraActivity.class));
        // TODO: slide camera in from right
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
        Location lastLocation = locationServices.getLastLocation();
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
