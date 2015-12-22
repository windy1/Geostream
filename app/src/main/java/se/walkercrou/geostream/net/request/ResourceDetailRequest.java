package se.walkercrou.geostream.net.request;

import android.content.Context;

import java.io.IOException;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.response.ResourceResponse;

/**
 * Represents a request to the server that gets a single resource's details
 */
public class ResourceDetailRequest <T extends Resource> extends Request<ResourceResponse<T>> {
    protected final Context c;
    protected final Class<T> resourceClass;

    public ResourceDetailRequest(Context c, Class<T> resourceClass, String resourceName,
                                 int resourceId) {
        // "<serverUrl>/api/<resourceName>/<resourceId>/" (e.g. "http://example.com/api/post/42/")
        super(Request.RESOURCE_ROOT + resourceName + '/' + resourceId + '/');
        this.c = c;
        this.resourceClass = resourceClass;
    }

    @Override
    public ResourceResponse<T> send() throws IOException {
        // Simple HTTP GET request, JSON expected as response
        return new ResourceResponse(c, resourceClass, (HttpURLConnection) url.openConnection());
    }
}
