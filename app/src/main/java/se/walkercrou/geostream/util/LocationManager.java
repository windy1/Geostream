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

    public LocationManager(Context c) {
        this.c = c;
        // build google api client
        client = new Builder(c)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
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
        return LocationServices.FusedLocationApi.getLastLocation(client);
    }

    @Override
    public void onConnected(Bundle bundle) {
        G.d("Connected to location services API");
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
        Dialogs.locationError(c, (dialog, which) -> connect(callback)).show();
    }
}
