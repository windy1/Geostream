package se.walkercrou.geostream;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import se.walkercrou.geostream.util.AppUtil;
import se.walkercrou.geostream.util.DialogUtil;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

/**
 * A wrapper class for handling the location services API.
 *
 * TODO: provide regular location updates
 * TODO: move map with location updates
 */
public class LocationManager implements ConnectionCallbacks, OnConnectionFailedListener{
    private GoogleApiClient client;
    private Location lastLocation;
    private Runnable callback;
    private Context c;

    public LocationManager(Context c) {
        this.c = c;
        // build google api client
        client = new Builder(c)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(com.google.android.gms.location.LocationServices.API)
                .build();
    }

    /**
     * Attempts to connect to the API and then runs the specified callback on success.
     *
     * @param callback to call when connected
     */
    public void connect(Runnable callback) {
        this.callback = callback;
        client.connect();
    }

    /**
     * Attempts to connect to the API.
     */
    public void connect() {
        connect(null);
    }

    /**
     * Returns the last known location.
     *
     * @return last location
     */
    public Location getLastLocation() {
        return lastLocation;
    }

    @Override
    public void onConnected(Bundle bundle) {
        AppUtil.d("Connected to location services API");
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(client);
        AppUtil.d("Last location : %s,%s", lastLocation.getLatitude(), lastLocation.getLongitude());
        if (callback != null)
            callback.run();
    }

    @Override
    public void onConnectionSuspended(int i) {
        showLocationErrorDialog();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        showLocationErrorDialog();
    }

    private void showLocationErrorDialog() {
        DialogUtil.locationError(c, (dialog, which) -> connect(callback)).show();
    }
}
