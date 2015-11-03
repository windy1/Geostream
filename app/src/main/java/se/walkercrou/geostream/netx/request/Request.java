package se.walkercrou.geostream.netx.request;

import android.os.AsyncTask;

import java.io.IOException;

import se.walkercrou.geostream.netx.response.Response;
import se.walkercrou.geostream.util.G;

/**
 * Represents a request to the Geostream server
 *
 * @param <T> type of response expected
 */
public abstract class Request<T extends Response> {
    protected String url = G.app.serverUrl;

    /**
     * Creates a new Request with the specified route. The route is appended to the server url to
     * build the request url.
     *
     * @param route of request
     */
    public Request(String route) {
        // ensure leading and trailing slashes
        if (!route.startsWith("/")) route = '/' + route;
        if (!route.endsWith("/")) route += '/';
        url += route;
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
