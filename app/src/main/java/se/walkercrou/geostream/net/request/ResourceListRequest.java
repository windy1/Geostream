package se.walkercrou.geostream.net.request;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import se.walkercrou.geostream.net.response.ResourceResponse;

/**
 * Represents a request to the server that gets a list of resources
 */
public class ResourceListRequest extends Request<ResourceResponse> {
    public ResourceListRequest(String resourceName) throws MalformedURLException {
        // "<serverUrl>/api/<resourceName>/" (e.g. "http://example.com/api/posts/")
        super(Request.ROUTE_ROOT + resourceName + '/');
    }

    @Override
    public ResourceResponse send() throws IOException {
        // Simple HTTP GET request, JSON expected as response
        return new ResourceResponse((HttpURLConnection) url.openConnection());
    }
}
