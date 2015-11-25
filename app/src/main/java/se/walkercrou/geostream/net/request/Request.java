package se.walkercrou.geostream.net.request;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import se.walkercrou.geostream.net.response.Response;
import se.walkercrou.geostream.util.G;

/**
 * Represents a request to the Geostream server
 *
 * @param <T> type of response expected
 */
public abstract class Request<T extends Response> {
    public static final String RESOURCE_ROOT = "/api/";
    public static final String MEDIA_ROOT = "/media/";

    protected URL url;

    /**
     * Creates a new Request with the specified route. The route is appended to the server url to
     * build the request url.
     *
     * @param route of request
     */
    public Request(String route, boolean includeServerUrl) {
        try {
            if (includeServerUrl) {
                // ensure leading and trailing slashes
                if (!route.startsWith("/")) route = '/' + route;
                if (!route.endsWith("/")) route += '/';
                url = new URL(G.app.serverUrl + route);
            } else
                url = new URL(route);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new Request with the specified route. The route is appended to the server url to
     * build the request url.
     *
     * @param route of request
     */
    public Request(String route) {
        this(route, true);
    }

    /**
     * Sends this request to the server
     *
     * @return response
     * @throws IOException see implementing class
     */
    public abstract T send() throws IOException;

    /**
     * Sends this request in a background task.
     *
     * @return response
     */
    public T sendInBackground() {
        try {
            G.d("Request: " + url);
            return new AsyncTask<Void, Void, T>() {
                @Override
                protected T doInBackground(Void... params) {
                    try {
                        return send();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.execute().get();
        } catch (Exception e) {
            G.e("An error occurred while trying to send a request to the server.", e);
            return null;
        }
    }
}
