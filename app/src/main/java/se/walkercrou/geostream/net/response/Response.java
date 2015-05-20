package se.walkercrou.geostream.net.response;

import java.io.InputStream;

import se.walkercrou.geostream.net.ServerConnection;

/**
 * Represents a response from the server. All implementations must implement an int, String,
 * InputStream constructor.
 *
 * @param <T> type of object the server is returning
 */
public abstract class Response<T> {
    protected final InputStream in;
    protected final int statusCode;
    protected final String statusMessage;

    public Response(int statusCode, String statusMessage, InputStream in) {
        this.in = in;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    /**
     * Returns the object that the server returned.
     *
     * @return object server returned
     */
    public abstract T get();

    /**
     * Returns the status code of the request.
     *
     * @return status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns true if an error occurred.
     *
     * @return true if error occurred
     */
    public boolean isError() {
        return ServerConnection.isStatusError(statusCode);
    }

    /**
     * Returns the status message returned by the server.
     *
     * @return status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public String toString() {
        return "----- " + statusCode + " " + statusMessage + " -----";
    }
}
