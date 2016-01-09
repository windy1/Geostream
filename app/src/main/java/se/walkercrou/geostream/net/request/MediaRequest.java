package se.walkercrou.geostream.net.request;

import android.content.Context;

import java.io.IOException;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.net.response.MediaResponse;
import se.walkercrou.geostream.post.Post;

/**
 * Represents a request for media on the server
 */
public class MediaRequest extends Request<MediaResponse> {
    private final Context c;

    public MediaRequest(Context c, String route) {
        super(route, !route.startsWith("http://"));
        this.c = c;
    }

    @Override
    public MediaResponse send() throws IOException {
        // just a simple GET request here
        boolean video = url.toString().endsWith(Post.VIDEO_FILE_EXTENSION + '/') ||
                url.toString().endsWith(Post.VIDEO_FILE_EXTENSION);
        return new MediaResponse(c, video, getConnection());
    }
}
