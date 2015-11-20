package se.walkercrou.geostream.net.request;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import se.walkercrou.geostream.net.response.ResourceResponse;

/**
 * Represents a request to delete a resource on the server. This extends
 * {@link ResourceDetailRequest} because it follows the same URL pattern. A client secret must be
 * provided in order to delete a resource on the server which is received when the resource is
 * created.
 */
public class ResourceDeleteRequest extends ResourceDetailRequest {
    private final String clientSecret;

    public ResourceDeleteRequest(String resourceName, int resourceId, String clientSecret)
        throws MalformedURLException {
        super(resourceName, resourceId);
        this.clientSecret = clientSecret;
    }

    @Override
    public ResourceResponse send() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("ClientSecret", clientSecret);
        return new ResourceResponse(conn);
    }
}
