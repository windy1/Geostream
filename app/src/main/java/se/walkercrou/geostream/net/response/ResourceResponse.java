package se.walkercrou.geostream.net.response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.util.G;

/**
 * Represents a response from the server that has to do with server resources
 */
public class ResourceResponse extends Response<Object> {
    private static final String FIELD_ERROR_DETAIL = "detail";
    private JSONObject obj;
    private JSONArray array;

    public ResourceResponse(InputStream in, int statusCode, String statusMessage) {
        super(in, statusCode, statusMessage);

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            // read response
            String ln;
            while ((ln = reader.readLine()) != null)
                builder.append(ln).append("\n");

            // close reader and input stream
            reader.close();
            in.close();

            // build json object
            obj = new JSONObject(builder.toString());
        } catch (IOException e) {
            G.e("An error occurred while trying to read the server's response", e);
        } catch (JSONException e) {
            // not a JSONObject try to build an array instead
            try {
                array = new JSONArray(builder.toString());
            } catch (JSONException e1) {
                // empty response body
            }
        }
    }

    public ResourceResponse(HttpURLConnection conn) throws IOException {
        this(conn.getInputStream(), conn.getResponseCode(), conn.getResponseMessage());
    }

    /**
     * Returns a detail message sent by the server if there was an error
     *
     * @return error message
     */
    public String getErrorDetail() {
        try {
            if (obj != null)
                return obj.getString(FIELD_ERROR_DETAIL);
            return null;
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public Object get() {
        return obj != null ? obj : array;
    }

    @Override
    public String toString() {
        try {
            if (obj != null)
                return obj.toString(2);
            else if (array != null)
                return array.toString(2);
            else
                return super.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return super.toString();
        }
    }
}
