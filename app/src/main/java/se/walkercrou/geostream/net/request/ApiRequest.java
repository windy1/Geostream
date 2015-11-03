package se.walkercrou.geostream.net.request;

import se.walkercrou.geostream.net.ServerConnection;
import se.walkercrou.geostream.net.response.ApiResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents a request to the server's RESTful API.
 */
public class ApiRequest extends Request<ApiResponse> {
    // request URLs
    public static final String URL_POST_LIST = "/api/posts/";
    public static final String URL_POST_DETAIL = "/api/posts/%s/";

    public static final String HEADER_CLIENT_SECRET = "Client-Secret";

    public ApiRequest(String method, String relativeUrl) {
        super(method, G.app.serverUrl + relativeUrl);
    }

    public ApiRequest(String method, String relativeUrl, String lookup) {
        super(method, G.app.serverUrl + relativeUrl, lookup);
    }

    @Override
    public ApiRequest set(String name, Object value) {
        return (ApiRequest) super.set(name, value);
    }

    @Override
    public ApiRequest addExtraHeader(String name, String value) {
        return (ApiRequest) super.addExtraHeader(name, value);
    }

    @Override
    public ApiResponse send() {
        // attach query string if GET method
        String url = this.url;
        if (method.equals(METHOD_GET) && !data.isEmpty())
            url += '?' + queryString();

        // create server connection
        ServerConnection<ApiResponse> conn = new ServerConnection<>(url, ApiResponse.class)
                .method(method);

        // write form data if posting and have data
        if (!method.equals(METHOD_GET) && !data.isEmpty())
            conn.writeFormData(data);

        ApiResponse response = conn.connect();
        if (response != null)
            G.d(response);
        conn.disconnect();
        return response;
    }
}
