package se.walkercrou.geostream.net.request;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import se.walkercrou.geostream.net.response.ResourceResponse;

/**
 * Represents a request to the server to create a new resource. This request uses
 * multipart/form-data in writing it's data and is received by the server the same way as if a web
 * form was used. Although the server accepts JSON requests, this is necessary in order to send file
 * data. This class extends {@link ResourceListRequest} because it follows the same URL pattern (no
 * resource ID required).
 */
public class ResourceCreateRequest extends ResourceListRequest {
    // Internal map of data to write
    private final Map<String, Object> parameters = new HashMap<>();

    public ResourceCreateRequest(String resourceName) {
        super(resourceName);
    }

    /**
     * Sets the value of a parameter to write to the form.
     *
     * @param param to write
     * @param value value of param
     * @return this request
     */
    public ResourceCreateRequest set(String param, Object value) {
        parameters.put(param, value);
        return this;
    }

    @Override
    public ResourceResponse send() throws IOException {
        // obtain server connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setDoOutput(true);

        // form writing constants
        final String lineEnd = "\r\n";
        final String twoHyphens = "--";
        final String boundary = "GeostreamFormBoundary" + System.currentTimeMillis();

        // configure connection
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        // start request
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        // write form boundary (separates form data)
        out.writeBytes(twoHyphens + boundary + lineEnd);
        for (String param : parameters.keySet()) {
            // start form data, write content disposition
            String contentDisposition = "Content-Disposition: form-data; name=\"" + param + '"';
            Object value = parameters.get(param);
            if (value instanceof FileValue) {
                // parameter value is a file, add extra file info to content disposition
                FileValue fileValue = (FileValue) value;
                contentDisposition += ";filename=\"" + fileValue.name + '"' + lineEnd;
                out.writeBytes(contentDisposition);
                out.writeBytes(lineEnd); // blank line between CD and data
                out.write(fileValue.data);
            } else {
                contentDisposition += lineEnd;
                out.writeBytes(contentDisposition);
                out.writeBytes(lineEnd); // blank line between CD and data
                out.writeBytes(value.toString());
            }
            // end form data
            out.writeBytes(lineEnd);
            out.writeBytes(twoHyphens + boundary + lineEnd);
        }

        // close output stream
        out.flush();
        out.close();

        return new ResourceResponse(conn);
    }

    /**
     * Represents a File that will be written to a multipart/form-data HTTP request.
     */
    public static class FileValue {
        private final String name, type;
        private final byte[] data;

        public FileValue(String name, String type, byte[] data) {
            this.name = name;
            this.type = type;
            this.data = data;
        }
    }
}
