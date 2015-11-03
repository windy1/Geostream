package se.walkercrou.geostream.net.request;

import se.walkercrou.geostream.net.ServerConnection;
import se.walkercrou.geostream.net.response.MediaResponse;
import se.walkercrou.geostream.util.G;

/**
 * A request for a media resource on the server.
 */
public class MediaRequest extends Request<MediaResponse> {
    public MediaRequest(String fileUrl) {
        super(Request.METHOD_GET, fileUrl);
    }

    @Override
    public MediaResponse send() {
        // initialize server connection
        ServerConnection<MediaResponse> conn = new ServerConnection<>(url, MediaResponse.class)
                .method(Request.METHOD_GET);

        // send request and receive response
        MediaResponse response = conn.connect();
        if (response != null)
            G.d(response);
        conn.disconnect();
        return response;
    }
}
