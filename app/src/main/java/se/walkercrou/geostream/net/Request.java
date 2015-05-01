package se.walkercrou.geostream.net;

import android.os.AsyncTask;
import android.util.Base64;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import se.walkercrou.geostream.App;

/**
 * Represents a request to be made to the server over a {@link ServerConnection}.
 */
public class Request {
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";

    public static final String URL_POST_LIST = "/api/posts/";
    public static final String URL_POST_DETAIL = "/api/posts/%s/";
    public static final String URL_POST_MEDIA = "/media/%s/";
    public static final String URL_USER_LIST = "/api/users/";
    public static final String URL_USER_DETAIL = "/api/users/%s/";
    public static final String URL_SIGNUP = "/api/signup/";

    private final String method, relativeUrl;
    private String authEncoding;
    private final Map<String, Object> data = new HashMap<>();

    public Request(String method, String relativeUrl) {
        this.method = method;
        this.relativeUrl = relativeUrl;
    }

    public Request(String method, String relativeUrl, String lookup) {
        this.method = method;
        this.relativeUrl = String.format(relativeUrl, lookup);
    }

    /**
     * Sets a parameter for the request
     *
     * @param name to set
     * @param value to set
     */
    public Request set(String name, Object value) {
        data.put(name, value);
        return this;
    }

    /**
     * Returns the key value pairs for request.
     *
     * @return key value pairs
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Returns the method that should be used.
     *
     * @return method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the url relative to the server root.
     *
     * @return url relative to server root
     */
    public String getRelativeUrl() {
        return relativeUrl;
    }

    /**
     * Returns a query string of the request's data. To be used with GET requests.
     *
     * @return query string
     */
    public String queryString() {
        try {
            String q = "";
            for (String name : data.keySet()) {
                if (!q.isEmpty())
                    q += '&';
                q += name + "=" + URLEncoder.encode(data.get(name).toString(), "UTF-8");
            }
            return q;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sets the authorization of this request.
     *
     * @param username username
     * @param password password
     */
    public Request setAuthorization(String username, String password) {
        authEncoding = Base64.encodeToString((username + ":" + password).getBytes(),
                Base64.DEFAULT);
        return this;
    }

    /**
     * Sends this request to the server.
     *
     * @return response
     */
    public Response send() {
        // attach query string if GET method
        String url = relativeUrl;
        if (method.equals(METHOD_GET) && !data.isEmpty())
            url += '?' + queryString();

        // connect to server
        ServerConnection conn = new ServerConnection(url);
        if (!conn.connect())
            return null;

        // add auth header if basic auth
        if (authEncoding != null)
            conn.auth(authEncoding);

        // send request and read response
        Response response = conn.sendRequest(this);
        if (response != null)
            App.d(response);
        conn.disconnect();
        return response;
    }

    /**
     * Sends this request to the server in an {@link AsyncTask}.
     *
     * @return response
     */
    public Response sendInBackground() {
        try {
            return new AsyncTask<Void, Void, Response>() {
                @Override
                protected Response doInBackground(Void... params) {
                    return send();
                }
            }.execute().get();
        } catch (Exception e) {
            App.e("An error occurred while trying to send a request to the server.", e);
            return null;
        }
    }
}
