package se.walkercrou.geostream.net.response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.util.G;

/**
 * Represents a response from the server containing a single or collection of resources.
 * @param <T> resource class
 */
public class ResourceResponse<T extends Resource> extends Response<T> {
    private final List<T> results = new ArrayList<>();
    private JSONObject obj;
    private JSONArray array;
    private String errorDetail;

    private static final String FIELD_ERROR_DETAIL = "detail";

    public ResourceResponse(Class<T> resourceType, InputStream in, int statusCode, String statusMessage) {
        super(in, statusCode, statusMessage);

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        // get "parse" static method in Resource class
        Method parser;
        try {
            parser = resourceType.getMethod("parse", JSONObject.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        try {
            // read response
            String ln;
            while ((ln = reader.readLine()) != null)
                builder.append(ln).append('\n');

            reader.close();
            in.close();

            // build json object and feed to parse method in resource class
            obj = new JSONObject(builder.toString());
            if (isError())
                errorDetail = obj.getString(FIELD_ERROR_DETAIL);
            else
                results.add((T) parser.invoke(null, obj));
        } catch (JSONException e) {
            try {
                // parse each json object within the array
                array = new JSONArray(builder.toString());
                for (int i = 0; i < array.length(); i++)
                    results.add((T) parser.invoke(null, array.get(i)));
            } catch (JSONException f) {
                // ignore further json exceptions
            } catch (Exception f) {
                throw new RuntimeException(f);
            }
        } catch (IOException e) {
            G.e("An error occurred while trying to read the server's response", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public ResourceResponse(Class<T> resourceType, HttpURLConnection conn)
            throws IOException {
        this(resourceType, conn.getInputStream(), conn.getResponseCode(), conn.getResponseMessage());
    }

    /**
     * Returns the "detail" string returned in the event of an error.
     *
     * @return error string
     */
    public String getErrorDetail() {
        return errorDetail;
    }

    /**
     * Returns the full result list.
     *
     * @return full result list
     */
    public List<T> getList() {
        return results;
    }

    @Override
    public T get() {
        return results.get(0);
    }

    @Override
    public String toString() {
        try {
            return obj != null ? obj.toString(4)
                    : array != null ? array.toString(4) : super.toString();
        } catch (JSONException e) {
            return super.toString();
        }
    }
}
