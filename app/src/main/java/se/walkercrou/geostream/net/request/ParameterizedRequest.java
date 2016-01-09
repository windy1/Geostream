package se.walkercrou.geostream.net.request;

/**
 * Represents a Request that has parameters to be passed to the server.
 */
public interface ParameterizedRequest {
    /**
     * Sets the value of a parameter to write to the request.
     *
     * @param param to write
     * @param value value of param
     * @return this request
     */
    ParameterizedRequest set(String param, Object value);
}
