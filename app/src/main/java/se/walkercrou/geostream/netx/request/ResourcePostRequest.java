package se.walkercrou.geostream.netx.request;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import se.walkercrou.geostream.netx.response.ResourceResponse;

/**
 * Represents a HTTP "POST" request
 */
public class ResourcePostRequest extends ResourceRequest {
    private final JSONObject body = new JSONObject();

    public ResourcePostRequest(String path) {
        super(path);
    }

    public ResourcePostRequest(String path, Object... params) {
        super(path, params);
    }

    /**
     * Adds the specified value at the specified field to the JSON body of the request.
     *
     * @param field to put
     * @param value of of field
     * @return this request
     * @throws JSONException if malformed json
     */
    public ResourcePostRequest put(String field, Object value) throws JSONException {
        body.put(field, value);
        return this;
    }

    @Override
    public ResourceResponse send() throws IOException {
        // create url object
        URL url = new URL(this.url);
        // open connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // encode request body
        String encodedData = URLEncoder.encode(body.toString(), "UTF-8");

        // configure connection
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", String.valueOf(encodedData.length()));

        // write body
        OutputStream out = conn.getOutputStream();
        out.write(encodedData.getBytes());

        // build response
        int statusCode = conn.getResponseCode();
        String statusMessage = conn.getResponseMessage();
        InputStream in = conn.getInputStream();
        return new ResourceResponse(in, statusCode, statusMessage);
    }
}
