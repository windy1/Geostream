package se.walkercrou.geostream.net.request;

import java.io.IOException;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.net.response.MediaResponse;

/**
 * Represents a request for media on the server
 */
public class MediaRequest extends Request<MediaResponse> {
    public MediaRequest(String route) {
        super(route, !route.startsWith("http://"));
    }

    @Override
    public MediaResponse send() throws IOException {
        // just a simple GET request here
        return new MediaResponse((HttpURLConnection) url.openConnection());
    }
}
