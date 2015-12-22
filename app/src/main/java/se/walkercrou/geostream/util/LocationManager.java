package se.walkercrou.geostream.util;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

/**
 * A wrapper class for handling the location services API.
 */
public class LocationManager implements ConnectionCallbacks, OnConnectionFailedListener {
    private GoogleApiClient client;
    private Runnable callback;
    private Context c;

    /**
     * Attempts to connect to the API and then runs the specified callback on success.
     *
     * @param callback to call when connected
     */
    public void connect(Context c, Runnable callback) {
        G.d("connecting");
        // build google api client
        client = new Builder(c)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        client.connect();
        this.callback = callback;
    }

    /**
     * Attempts to connect to the API.
     */
    public void connect(Context c) {
        connect(c, null);
    }

    /**
     * Returns the last known location.
     *
     * @return last location
     */
    public Location getLastLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(client);
    }

    @Override
    public void onConnected(Bundle bundle) {
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
        E.location(c, (dialog, which) -> connect(c, callback)).show();
    }
}
