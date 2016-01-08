package se.walkercrou.geostream.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

/**
 * A wrapper class for handling the location services API.
 */
public class LocationApi {
    private final Activity a;
    private GoogleApiClient client;

    public LocationApi(Activity a) {
        this.a = a;
    }

    /**
     * Ensures that we have permission for location services and connects to the API.
     *
     * @param callback1 success callback
     * @param callback2 failed callback
     * @return true if had permission
     */
    public boolean connect(ConnectionCallbacks callback1, OnConnectionFailedListener callback2) {
        // check for location permission
        if (ActivityCompat.checkSelfPermission(a, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(a, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(a,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 862);
            return false;
        }

        client = new Builder(a, callback1, callback2).addApi(LocationServices.API).build();
        client.connect();

        return true;
    }

    /**
     * Disconnects from the API.
     */
    public void disconnect() {
        if (client != null)
            client.disconnect();
    }

    /**
     * Returns the last known location of the device.
     *
     * @return last location
     */
    public Location getLastLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(client);
    }

    /**
     * Notifies the API that it should start sending location updates to the specified
     * {@link LocationListener}.
     *
     * @param listener to send updates to
     */
    public void startLocationUpdates(LocationListener listener) {
        LocationRequest request = new LocationRequest();
        request.setInterval(5000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(client, request, listener);
    }
}
