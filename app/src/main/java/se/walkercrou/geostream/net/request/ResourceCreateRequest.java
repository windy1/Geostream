package se.walkercrou.geostream.net.request;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents a request to the server to create a new resource. This request uses
 * multipart/form-data in writing it's data and is received by the server the same way as if a web
 * form was used. Although the server accepts JSON requests, this is necessary in order to send file
 * data. This class extends {@link ResourceListRequest} because it follows the same URL pattern (no
 * resource ID required).
 */
public class ResourceCreateRequest extends ResourceListRequest {
    // Form boundary to seperate parameter data
    private final String boundary = "----" + G.app.name + "FormBoundary"
        + System.currentTimeMillis() + '\n';
    // Content type descriptor
    private final String contentType = "multipart/form-data; boundary=" + boundary;
    // Internal map of data to write
    private final Map<String, Object> parameters = new HashMap<>();

    public ResourceCreateRequest(String resourceName) throws MalformedURLException {
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
        // configure connection for multipart/form-data
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Content-Type", contentType);

        // start request
        conn.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        writeBytes(out, boundary);
        for (String param : parameters.keySet()) {
            writeBytes(out, "Content-Disposition: form-data; name=\"" + param + "\"");
            Object value = parameters.get(param);
            if (value instanceof FileValue) {
                // Parameter is a file
                FileValue fileValue = (FileValue) value;
                // Include extra descriptors
                writeBytes(out, "; filename=\"" + fileValue.name + "\"\n");
                writeBytes(out, "Content-Type: " + fileValue.type + "\n\n");
                // write data
                out.write(fileValue.data);
                G.dNoLn("<FILE_DATA>");
            } else {
                // write data
                writeBytes(out, "\n\n");
                writeBytes(out, value.toString());
            }
            writeBytes(out, boundary);
        }

        return new ResourceResponse(conn);
    }

    private void writeBytes(DataOutputStream out, String bytes) throws IOException {
        out.writeBytes(bytes);
        G.dNoLn(bytes);
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
