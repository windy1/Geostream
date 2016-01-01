package se.walkercrou.geostream.net.request;

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
