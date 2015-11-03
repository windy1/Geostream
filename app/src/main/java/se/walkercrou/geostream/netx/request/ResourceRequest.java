package se.walkercrou.geostream.netx.request;

import se.walkercrou.geostream.netx.response.ResourceResponse;

/**
 * Represents a request to the server that deals with server resources.
 */
public abstract class ResourceRequest extends Request<ResourceResponse> {
    /**
     * Route to the list of posts on the server
     */
    public static final String ROUTE_POST_LIST = "/api/posts/";
    /**
     * Route to a post's details on the server
     */
    public static final String ROUTE_POST_DETAIL = "/api/posts/%s/";

    public ResourceRequest(String route) {
        super(route);
    }

    public ResourceRequest(String route, Object... params) {
        this(String.format(route, params));
    }
}
