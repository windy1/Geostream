package se.walkercrou.geostream.net;

/**
 * Represents a response sent from the server.
 */
public class Response {

    // Some HTTP response codes
    public static int CODE_OK = 200;
    public static int CODE_CREATED = 201;

    public static int FIRST_ERROR_CODE = 300;

    private final int responseCode;
    private final String responseMessage;
    private final Object body;

    public Response(int responseCode, String responseMessage, Object body) {
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
    public Object getBody() {
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
        return code < CODE_OK || code >= FIRST_ERROR_CODE;
    }

    @Override
    public String toString() {
        try {
            return "----- " + responseCode + " " + responseMessage + " -----\n"
                    + body.toString();
        } catch (Exception e) {
            return super.toString();
        }
    }
}
