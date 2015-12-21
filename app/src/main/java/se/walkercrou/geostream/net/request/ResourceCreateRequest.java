package se.walkercrou.geostream.net.request;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.response.ResourceResponse;

/**
 * Represents a request to the server to create a new resource. This request uses
 * multipart/form-data in writing it's data and is received by the server the same way as if a web
 * form was used. Although the server accepts JSON requests, this is necessary in order to send file
 * data. This class extends {@link ResourceListRequest} because it follows the same URL pattern (no
 * resource ID required).
 */
public class ResourceCreateRequest <T extends Resource> extends ResourceListRequest<T> {
    // Internal map of data to write
    private final Map<String, Object> parameters = new HashMap<>();

    public ResourceCreateRequest(Class<T> resourceClass, String resourceName) {
        super(resourceClass, resourceName);
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
    public ResourceResponse<T> send() throws IOException {
        // obtain server connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setDoOutput(true);

        // form writing constants
        final String newLine = "\r\n";
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
        out.writeBytes(twoHyphens + boundary + newLine);
        for (String param : parameters.keySet()) {
            // start form data, write content disposition
            String contentDisposition = "Content-Disposition: form-data; name=\"" + param + '"';
            Object value = parameters.get(param);
            if (value instanceof MediaData) {
                // parameter value is a file, add extra file info to content disposition
                MediaData mediaData = (MediaData) value;
                contentDisposition += ";filename=\"" + mediaData.name + '"' + newLine;
                out.writeBytes(contentDisposition);
                out.writeBytes(newLine); // blank line between CD and data
                // write file data to stream
                if (mediaData.data instanceof File) {
                    // data is in the form of a file (video)
                    File file = (File) mediaData.data;
                    byte[] buffer = new byte[(int) file.length()];
                    FileInputStream fin = new FileInputStream(file);
                    fin.read(buffer);
                    fin.close();
                    out.write(buffer);
                } else
                    // file is already a raw byte array
                    out.write((byte[]) mediaData.data);
            } else {
                contentDisposition += newLine;
                out.writeBytes(contentDisposition);
                out.writeBytes(newLine); // blank line between CD and data
                out.writeBytes(value.toString());
            }
            // end form data
            out.writeBytes(newLine);
            out.writeBytes(twoHyphens + boundary + newLine);
        }

        // close output stream
        out.flush();
        out.close();

        return new ResourceResponse(resourceClass, conn);
    }

    /**
     * Represents a File that will be written to a multipart/form-data HTTP request.
     */
    public static class MediaData {
        private final String name;
        private final Object data;
        private final boolean video;

        public MediaData(String name, File data) {
            this.name = name;
            this.data = data;
            video = true;
        }

        public MediaData(String name, byte[] data) {
            this.name = name;
            this.data = data;
            video = false;
        }
    }
}
