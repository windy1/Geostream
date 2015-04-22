package se.walkercrou.geostream.net;

import android.os.AsyncTask;
import android.util.Base64;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import se.walkercrou.geostream.util.App;

/**
 * Represents a request to be made to the server over a {@link ServerConnection}.
 */
public class Request {
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";

    private final String method, relativeUrl;
    private String authEncoding;
    private final Map<String, Object> data = new HashMap<>();

    public Request(String method, String relativeUrl) {
        this.method = method;
        this.relativeUrl = relativeUrl;
    }

    /**
     * Sets a parameter for the request
     *
     * @param name to set
     * @param value to set
     */
    public void set(String name, Object value) {
        data.put(name, value);
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
    public void setAuthorization(String username, String password) {
        authEncoding = Base64.encodeToString((username + ":" + password).getBytes(),
                Base64.DEFAULT);
    }

    /**
     * Sends this request to the server.
     *
     * @return response
     */
    public Response send() {
        // attach query string if GET method
        String url = relativeUrl;
        if (method.equals(METHOD_GET))
            url += '?' + queryString();

        // connect to server
        ServerConnection conn = new ServerConnection(url);
        if (!conn.connect())
            return null;

        // add auth header if basic auth
        if (authEncoding != null)
            conn.auth(authEncoding);

        // send request if POST method
        if (method.equals(METHOD_POST))
            conn.sendRequest(this);

        // return the response
        Response response = conn.readResponse();
        App.d(response.toString());
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
