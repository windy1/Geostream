package se.walkercrou.geostream.net.response;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.net.ErrorCallback;

/**
 * Represents a response from the server
 *
 * @param <T> type of object returned
 */
public abstract class Response<T> {
    protected final InputStream in;
    protected final int statusCode;
    protected final String statusMessage;

    // HTTP status codes
    public static final int STATUS_OK = 200;
    public static final int FIRST_ERROR_STATUS = 300;

    /**
     * Creates a new Response with the given HTTP InputStream and status code and message
     *
     * @param in            input stream
     * @param statusCode    code of response
     * @param statusMessage message of response
     */
    public Response(InputStream in, int statusCode, String statusMessage) {
        this.in = in;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    /**
     * Creates a new Response from the given {@link HttpURLConnection}.
     *
     * @param conn to parse
     * @throws IOException if there is an error with the connection
     */
    public Response(HttpURLConnection conn) throws IOException {
        this(conn.getInputStream(), conn.getResponseCode(), conn.getResponseMessage());
    }

    /**
     * Returns the object returned by the server and parsed by the response object
     *
     * @return object parsed by client
     */
    public abstract T get();

    /**
     * Returns the HTTP status code of the response
     *
     * @return http status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns true if there was an error in the request
     *
     * @return true if error
     */
    public boolean isError() {
        return isStatusError(statusCode);
    }

    /**
     * Returns the HTTP status message of the response
     *
     * @return http status message
     */
    public String getErrorDetail() {
        return statusMessage;
    }

    /**
     * Returns true if the specified status code is an error.
     *
     * @param code to check
     * @return true if error
     */
    public static boolean isStatusError(int code) {
        return code < STATUS_OK || code >= FIRST_ERROR_STATUS;
    }

    /**
     * Checks if the specified response is not null and doesn't have an error. Returns true if no
     * error.
     *
     * @param response to check
     * @param callback in case of error
     * @return true if no error
     */
    public static boolean check(Response<?> response, ErrorCallback callback) {
        if (response == null) {
            callback.onError(null);
            return false;
        } else if (response.isError()) {
            callback.onError(response.getErrorDetail());
            return false;
        }
        return true;
    }
}
