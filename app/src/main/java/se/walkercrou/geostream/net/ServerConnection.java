package se.walkercrou.geostream.net;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

import se.walkercrou.geostream.net.request.FileValue;
import se.walkercrou.geostream.net.request.Request;
import se.walkercrou.geostream.net.response.Response;
import se.walkercrou.geostream.util.G;

/**
 * Represents a connection to a Geostream server
 */
public class ServerConnection<T extends Response> {
    // http request writing stuff
    private static final String crlf = "\r\n";
    private static final String twoHyphens = "--";
    private static final String boundary = G.app.name + "FormBoundary";

    private final String url;
    // the class to instantiate and return with input stream from server
    // the response class object then handles the input stream and objectifies what ever is being
    // returned
    private final Class<T> responseClass;
    private HttpURLConnection conn;
    // for sending POST data
    private DataOutputStream out;

    public ServerConnection(String url, Class<T> responseClass) {
        this.url = url;
        this.responseClass = responseClass;

        // initialize the connection
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
        } catch (Exception e) {
            G.e("An error occurred while trying to initialize a connection to the server", e);
        }

        // configure connection
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setInstanceFollowRedirects(false);

        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("charset", "UTF-8");
    }

    /**
     * Connects to the server and returns an {@link InputStream} connection.
     *
     * @return input stream
     */
    public T connect() {
        try {
            // get input stream
            G.d("Establishing connection to " + url);
            InputStream in;
            int code = conn.getResponseCode();
            if (isStatusError(code))
                in = conn.getErrorStream();
            else
                in = conn.getInputStream();
            G.d("Connection established");

            // create response
            return responseClass.getConstructor(
                    int.class, String.class, InputStream.class
            ).newInstance(code, conn.getResponseMessage(), in);
        } catch (Exception e) {
            G.e("An error occurred while trying to connect to the server", e);
            return null;
        }
    }

    /**
     * Severs the connection to the server.
     */
    public void disconnect() {
        conn.disconnect();
    }

    /**
     * Sets the HTTP method to use in the connection
     *
     * @param method to use when connecting
     * @return this connection
     */
    public ServerConnection method(String method) {
        try {
            conn.setRequestMethod(method);
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        // tell the server we are sending form data if a POST request
        if (method.equals(Request.METHOD_POST))
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        return this;
    }

    /**
     * Writes the specified mapped key-value pairs to the HTTP request to send upon connection.
     *
     * @param data to send
     */
    public void writeFormData(Map<String, Object> data) {
        try {
            // get output stream for connection
            conn.setDoOutput(true);
            out = new DataOutputStream(conn.getOutputStream());

            // write each data pair
            G.d("Sending data : " + data.toString());
            for (String name : data.keySet())
                writeData(name, data.get(name));

            // flush and close the output stream
            out.flush();
            out.close();
        } catch (Exception e) {
            G.e("An error occurred while trying to write form data to the server", e);
        }
    }

    private void writeBoundary() throws IOException {
        // separates form data in HTTP request
        out.writeBytes(twoHyphens + boundary + crlf);
    }

    private void writeData(String name, Object value) throws IOException {
        writeBoundary();
        String contentDisposition = "Content-Disposition: form-data; name=\"" + name + "\"";

        // check if the value is a file
        boolean file = false;
        if (value instanceof FileValue) {
            file = true;
            // add filename attribute if uploading a file
            FileValue fileValue = (FileValue) value;
            contentDisposition += ";filename=\"" + fileValue.getFileName() + "\"";
            value = fileValue.getData();
        }
        contentDisposition += crlf;

        // write data
        out.writeBytes(contentDisposition);
        out.writeBytes(crlf);
        if (file)
            out.write((byte[]) value);
        else
            out.writeBytes(value.toString());
        writeBoundary();
    }

    /**
     * Returns true if the specified status code is an error.
     *
     * @param code to check
     * @return true if error
     */
    public static boolean isStatusError(int code) {
        return code < Request.STATUS_OK || code >= Request.FIRST_ERROR_STATUS;
    }
}
