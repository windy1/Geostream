package se.walkercrou.geostream.net.response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import se.walkercrou.geostream.util.AppUtil;

/**
 * Represents a response from the server's RESTful API.
 */
public class ApiResponse extends Response {
    public static final String ERROR_DETAIL = "detail";
    private String jsonString;

    public ApiResponse(int statusCode, String statusMessage, InputStream in) {
        super(statusCode, statusMessage, in);

        // body as string
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            String ln;
            while ((ln = reader.readLine()) != null)
                builder.append(ln).append('\n');

            // close the reader and input stream
            reader.close();
            in.close();
        } catch (Exception e) {
            AppUtil.e("An error occurred while trying to read the JSON response from the server", e);
        }

        jsonString = builder.toString();
    }

    /**
     * Returns the error detail returned by the server.
     *
     * @return error detail
     */
    public String getErrorDetail() {
        try {
            return new JSONObject(jsonString).getString(ERROR_DETAIL);
        } catch (Exception e) {
            AppUtil.e("An error occurred while trying to read the JSON error from the server", e);
            return null;
        }
    }

    @Override
    public Object get() {
        try {
            // build json
            JSONObject obj = null;
            JSONArray array = null;
            try {
                obj = new JSONObject(jsonString);
            } catch (JSONException e) {
                array = new JSONArray(jsonString);
            }
            return obj != null ? obj : array;
        } catch (Exception e) {
            AppUtil.e("An error occurred while reading a JSON response from the server", e);
            return null;
        }
    }
}
