package se.walkercrou.geostream.net.response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import se.walkercrou.geostream.util.AppUtil;

/**
 * Represents a response from the server's RESTful API. Can return either a {@link JSONObject} or
 * {@link JSONArray}.
 */
public class ApiResponse extends Response {
    public static final String ERROR_DETAIL = "detail";
    private JSONObject obj;
    private JSONArray array;

    public ApiResponse(int statusCode, String statusMessage, InputStream in) {
        super(statusCode, statusMessage, in);

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            // read response
            String ln;
            while ((ln = reader.readLine()) != null)
                builder.append(ln).append('\n');

            // close reader and input stream
            reader.close();
            in.close();

            // build json
            obj = new JSONObject(builder.toString());
        } catch (IOException e) {
            AppUtil.e("An error occurred while trying to read the server's response", e);
        } catch (JSONException e) {
            // not a JSONObject, try to build a JSONArray instead
            try {
                array = new JSONArray(builder.toString());
            } catch (JSONException e1) {
                AppUtil.e("The returned response was invalid JSON", e1);
            }
        }
    }

    /**
     * Returns the error detail returned by the server.
     *
     * @return error detail
     */
    public String getErrorDetail() {
        try {
            return obj.getString(ERROR_DETAIL);
        } catch (Exception e) {
            AppUtil.e("An error occurred while trying to read the JSON error from the server", e);
            return null;
        }
    }

    @Override
    public Object get() {
        return obj != null ? obj : array;
    }
}
