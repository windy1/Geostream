package se.walkercrou.geostream.net.request;

import android.os.AsyncTask;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import se.walkercrou.geostream.net.response.Response;
import se.walkercrou.geostream.util.AppUtil;

/**
 * Represents some request to send to the server
 *
 * @param <T> type of response expected
 */
public abstract class Request<T extends Response> {
    // HTTP allowed methods
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";

    // HTTP status codes
    public static final int STATUS_OK = 200;

    public static final int FIRST_ERROR_STATUS = 300;

    public final String method, url;
    protected final Map<String, Object> data = new HashMap<>();

    public Request(String method, String url) {
        this.method = method;
        this.url = url;
    }

    public Request(String method, String relativeUrl, String lookup) {
        this(method, String.format(relativeUrl, lookup));
    }

    /**
     * Sends this request to the server and returns the server's response as a {@link Response}.
     *
     * @return server response
     */
    public abstract T send();

    /**
     * Sends this request in another thread.
     *
     * @return server response
     * @see #send()
     */
    public T sendInBackground() {
        try {
            return new AsyncTask<Void, Void, T>() {
                @Override
                protected T doInBackground(Void... params) {
                    return send();
                }
            }.execute().get();
        } catch (Exception e) {
            AppUtil.e("An error occurred while trying to send a request to the server.", e);
            return null;
        }
    }

    /**
     * Sets the specified name value in the HTTP request.
     *
     * @param name  to set
     * @param value of name
     * @return this request
     */
    public Request set(String name, Object value) {
        data.put(name, value);
        return this;
    }

    /**
     * Returns this request's HTTP data that will be sent to the server.
     *
     * @return data
     */
    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Returns the HTTP method that this request will use.
     *
     * @return HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the URL that this Request is intended for.
     *
     * @return url of request
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns a query string representation of this request's data.
     *
     * @return query string of request
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
}
