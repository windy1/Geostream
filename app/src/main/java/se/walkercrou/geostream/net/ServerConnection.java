package se.walkercrou.geostream.net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

import se.walkercrou.geostream.App;

/**
 * Represents a connection to the Geostream server
 */
public class ServerConnection {
    public static final String ROOT_URL = "http://10.245.155.173:8000";

    // http request writing stuff
    private static final String crlf = "\r\n";
    private static final String twoHyphens = "--";
    private static final String boundary = "GeostreamFormBoundary";

    private final String relativeUrl;
    private HttpURLConnection conn;
    private DataOutputStream out;

    public ServerConnection(String relativeUrl) {
        this.relativeUrl = relativeUrl;
    }

    /**
     * Attempts to connect to the Geostream server.
     *
     * @return true if successful
     */
    public boolean connect() {
        // establish connection
        String uri = ROOT_URL + relativeUrl;
        App.d("Establishing connection to " + uri);
        try {
            URL url = new URL(uri);
            conn = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            App.e("An error occurred while trying to connect to the server", e);
            return false;
        }

        // configure connection
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("charset", "utf-8");

        return true;
    }

    /**
     * Adds a basic authentication header to the connection.
     *
     * @param encoding base64 encoding of username and password
     */
    public void auth(String encoding) {
        // add a basic auth header
        conn.setRequestProperty("Authorization", "Basic " + encoding);
    }

    /**
     * Sends a request to the server.
     *
     * @param request to send
     */
    public Response sendRequest(Request request) {
        try {
            App.d("Sending request to server");
            String method = request.getMethod();
            setRequestMethod(request.getMethod());
            if (method.equals(Request.METHOD_GET))
                return readResponse();

            // get output stream for connection
            conn.setDoOutput(true);
            out = new DataOutputStream(conn.getOutputStream());

            // write the request
            writeRequest(request);

            // flush and close the buffer
            out.flush();
            out.close();

            return readResponse();
        } catch (Exception e) {
            App.e("An error occurred while trying to write a request to the server.", e);
            return null;
        }
    }

    private Response readResponse() {
        try {
            App.d("Reading response from server");
            // get input stream to server
            InputStream in;
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300)
                in = conn.getErrorStream();
            else
                in = conn.getInputStream();

            App.d("Connection established");

            // read the response
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String ln;
            while ((ln = reader.readLine()) != null)
                builder.append(ln).append('\n');

            // close the reader and input stream
            reader.close();
            in.close();

            // build json
            String jsonString = builder.toString();
            JSONObject obj = null;
            JSONArray array = null;
            try {
                obj = new JSONObject(jsonString);
            } catch (JSONException e) {
                array = new JSONArray(jsonString);
            }

            // return a new response object
            return new Response(code, conn.getResponseMessage(), obj != null ? obj : array);
        } catch (Exception e) {
            App.e("An error occurred while trying to read the response from the server", e);
            return null;
        }
    }

    /**
     * Disconnects from the server
     */
    public void disconnect() {
        conn.disconnect();
    }

    private void setRequestMethod(String method) throws ProtocolException {
        conn.setRequestMethod(method);
        // if posting, tell the server we are serving it form data
        if (method.equals(Request.METHOD_POST))
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
    }

    private void writeBoundary() throws IOException {
        out.writeBytes(twoHyphens + boundary + crlf);
    }

    private void writeRequest(Request request) throws IOException {
        writeBoundary();
        Map<String, Object> data = request.getData();
        for (String name : data.keySet())
            writeData(name, data.get(name));
    }

    private void writeData(String name, Object value) throws IOException {
        String contentDisposition = "Content-Disposition: form-data; name=\"" + name + "\"";

        // check if the value is actually a file
        boolean file = false;
        if (value instanceof FileValue) {
            file = true;
            // add the filename attribute if uploading a file
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
}
