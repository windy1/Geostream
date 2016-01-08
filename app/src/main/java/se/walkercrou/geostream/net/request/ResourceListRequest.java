package se.walkercrou.geostream.net.request;

import android.content.Context;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents a request to the server that gets a list of resources
 */
public class ResourceListRequest<T extends Resource> extends Request<ResourceResponse<T>>
        implements ParameterizedRequest {
    protected final Context c;
    protected final Class<T> resourceClass;
    protected final Map<String, Object> params = new HashMap<>();

    public ResourceListRequest(Context c, Class<T> resourceClass, String resourceName,
                               String urlExtension) {
        // "<serverUrl>/api/<resourceName>/<urlExt>" (e.g. "http://example.com/api/posts/nearby/")
        super(Request.RESOURCE_ROOT + resourceName + '/'
                + (urlExtension == null ? "" : urlExtension + '/'));
        this.c = c;
        this.resourceClass = resourceClass;
    }

    public ResourceListRequest(Context c, Class<T> resourceClass, String resourceName) {
        this(c, resourceClass, resourceName, null);
    }

    @Override
    public ResourceListRequest<T> set(String param, Object value) {
        params.put(param, value);
        return this;
    }

    @Override
    public ResourceResponse<T> send() throws IOException {
        // append query to url
        StringBuilder urlBuilder = new StringBuilder(url.toString());
        boolean first = true;
        for (String param : params.keySet()) {
            if (first)
                urlBuilder.append('?');
            else
                urlBuilder.append('&');
            urlBuilder.append(param).append('=').append(params.get(param));
            first = false;
        }
        url = new URL(urlBuilder.toString());
        G.d("url = " + url);

        return new ResourceResponse(c, resourceClass, (HttpURLConnection) url.openConnection());
    }
}
