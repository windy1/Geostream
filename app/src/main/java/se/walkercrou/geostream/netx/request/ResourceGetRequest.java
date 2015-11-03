package se.walkercrou.geostream.netx.request;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import se.walkercrou.geostream.netx.response.ResourceResponse;

/**
 * Represents a "GET" request to the server.
 */
public class ResourceGetRequest extends ResourceRequest {
    public ResourceGetRequest(String path) {
        super(path);
    }

    public ResourceGetRequest(String path, Object... params) {
        super(path, params);
    }

    @Override
    public ResourceResponse send() throws IOException {
        // build url object
        URL url = new URL(this.url);
        // open connection to server
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // return response
        int statusCode = conn.getResponseCode();
        String statusMessage = conn.getResponseMessage();
        InputStream in = conn.getInputStream();
        return new ResourceResponse(in, statusCode, statusMessage);
    }
}
