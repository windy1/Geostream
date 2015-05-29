package se.walkercrou.geostream.util;

/**
 * Functional interface for error handling.
 */
public interface ErrorCallback {
    /**
     * Called when there is an error. A null error message usually signifies that a connection could
     * not be made to the server.
     *
     * @param error error message if any, null if not
     */
    void onError(String error);
}
