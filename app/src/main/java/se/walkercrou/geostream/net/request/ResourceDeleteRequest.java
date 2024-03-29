package se.walkercrou.geostream.net.request;

import android.content.Context;

import java.io.IOException;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.response.ResourceResponse;

/**
 * Represents a request to delete a resource on the server. This extends
 * {@link ResourceDetailRequest} because it follows the same URL pattern. A client secret must be
 * provided in order to delete a resource on the server which is received when the resource is
 * created.
 */
public class ResourceDeleteRequest<T extends Resource> extends ResourceDetailRequest<T> {
    private final String clientSecret;

    public ResourceDeleteRequest(Context c, Class<T> resourceClass, String resourceName, int resourceId,
                                 String clientSecret) {
        super(c, resourceClass, resourceName, resourceId);
        this.clientSecret = clientSecret;
    }

    @Override
    public ResourceResponse<T> send() throws IOException {
        HttpURLConnection conn = getConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("ClientSecret", clientSecret);
        return new ResourceResponse(c, resourceClass, conn);
    }
}
