package se.walkercrou.geostream.net.request;

import java.io.IOException;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.response.ResourceResponse;

/**
 * Represents a request to the server that gets a list of resources
 */
public class ResourceListRequest <T extends Resource> extends Request<ResourceResponse<T>> {
    protected final Class<T> resourceClass;

    public ResourceListRequest(Class<T> resourceClass, String resourceName) {
        // "<serverUrl>/api/<resourceName>/" (e.g. "http://example.com/api/posts/")
        super(Request.RESOURCE_ROOT + resourceName + '/');
        this.resourceClass = resourceClass;
    }

    @Override
    public ResourceResponse<T> send() throws IOException {
        // Simple HTTP GET request, JSON expected as response
        return new ResourceResponse(resourceClass, (HttpURLConnection) url.openConnection());
    }
}
