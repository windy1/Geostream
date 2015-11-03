package se.walkercrou.geostream.netx.response;

import java.io.InputStream;

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
     * @param in input stream
     * @param statusCode code of response
     * @param statusMessage message of response
     */
    public Response(InputStream in, int statusCode, String statusMessage) {
        this.in = in;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
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
    public String getStatusMessage() {
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
}
