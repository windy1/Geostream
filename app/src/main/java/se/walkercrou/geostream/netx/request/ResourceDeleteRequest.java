package se.walkercrou.geostream.netx.request;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import se.walkercrou.geostream.netx.response.ResourceResponse;

/**
 * Represents an HTTP "DELETE" request
 */
public class ResourceDeleteRequest extends ResourceRequest {
    private final String clientSecret;

    public ResourceDeleteRequest(String path, String clientSecret) {
        super(path);
        this.clientSecret = clientSecret;
    }

    public ResourceDeleteRequest(String path, String clientSecret, Object... params) {
        super(path, params);
        this.clientSecret = clientSecret;
    }

    @Override
    public ResourceResponse send() throws IOException {
        URL url = new URL(this.url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Client-Secret", clientSecret);

        int statusCode = conn.getResponseCode();
        String statusMessage = conn.getResponseMessage();
        InputStream in = conn.getInputStream();
        return new ResourceResponse(in, statusCode, statusMessage);
    }
}
