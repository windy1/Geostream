package se.walkercrou.geostream.net.request;

import java.io.IOException;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.net.response.ResourceResponse;

/**
 * Represents a request to the server that gets a single resource's details
 */
public class ResourceDetailRequest extends Request<ResourceResponse> {
    public ResourceDetailRequest(String resourceName, int resourceId) {
        // "<serverUrl>/api/<resourceName>/<resourceId>/" (e.g. "http://example.com/api/post/42/")
        super(Request.RESOURCE_ROOT + resourceName + '/' + resourceId + '/');
    }

    @Override
    public ResourceResponse send() throws IOException {
        // Simple HTTP GET request, JSON expected as response
        return new ResourceResponse((HttpURLConnection) url.openConnection());
    }
}
