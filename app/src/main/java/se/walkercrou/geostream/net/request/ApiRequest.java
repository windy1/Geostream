package se.walkercrou.geostream.net.request;

import se.walkercrou.geostream.App;
import se.walkercrou.geostream.net.ServerConnection;
import se.walkercrou.geostream.net.response.ApiResponse;

/**
 * Represents a request to the server's RESTful API.
 */
public class ApiRequest extends Request<ApiResponse> {
    // request URLs
    public static final String URL_POST_LIST = "/api/posts/";
    public static final String URL_POST_DETAIL = "/api/posts/%s/";
    public static final String URL_USER_LIST = "/api/users/";
    public static final String URL_USER_DETAIL = "/api/users/%s/";
    public static final String URL_SIGNUP = "/api/signup/";

    public ApiRequest(String method, String relativeUrl) {
        super(method, relativeUrl);
    }

    public ApiRequest(String method, String relativeUrl, String lookup) {
        super(method, relativeUrl, lookup);
    }

    @Override
    public ApiRequest set(String name, Object value) {
        return (ApiRequest) super.set(name, value);
    }

    @Override
    public ApiRequest setAuthorization(String username, String password) {
        return (ApiRequest) super.setAuthorization(username, password);
    }

    @Override
    public ApiResponse send() {
        // attach query string if GET method
        String url = relativeUrl;
        if (method.equals(METHOD_GET) && !data.isEmpty())
            url += '?' + queryString();

        // create server connection
        ServerConnection<ApiResponse> conn = new ServerConnection<>(url, ApiResponse.class)
                .method(method);
        if (authEncoding != null)
            conn.auth(authEncoding);

        // write form data if posting and have data
        if (method.equals(METHOD_POST) && !data.isEmpty())
            conn.writeFormData(data);

        ApiResponse response = conn.connect();
        if (response != null)
            App.d(response);
        return response;
    }
}
