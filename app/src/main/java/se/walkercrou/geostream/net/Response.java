package se.walkercrou.geostream.net;

import org.json.JSONObject;

/**
 * Represents a response sent from the server.
 */
public class Response {
    private final int responseCode;
    private final String responseMessage;
    private final JSONObject body;

    public Response(int responseCode, String responseMessage, JSONObject body) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.body = body;
    }

    /**
     * Returns the HTTP code sent with the response.
     *
     * @return response code
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Returns true if the response indicates that an error occurred. A response is considered an
     * error if it's response code is less than 200 or greater than or equal to 300
     *
     * @return true if error
     */
    public boolean isError() {
        return isError(responseCode);
    }

    /**
     * Returns the message of the response code.
     *
     * @return response code message
     */
    public String getResponseMessage() {
        return responseMessage;
    }

    /**
     * Returns the JSON body of the response.
     *
     * @return json body
     */
    public JSONObject getBody() {
        return body;
    }

    /**
     * Returns true if the response indicates that an error occurred. A response is considered an
     * error if it's response code is less than 200 or greater than or equal to 300
     *
     * @param code to check
     * @return true if error
     */
    public static boolean isError(int code) {
        return code < 200 || code >= 300;
    }

    @Override
    public String toString() {
        try {
            return "----- " + responseCode + " " + responseMessage + " -----\n"
                    + body.toString(4);
        } catch (Exception e) {
            return super.toString();
        }
    }
}
