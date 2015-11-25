package se.walkercrou.geostream.net.request;

import java.io.IOException;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.net.response.MediaResponse;

public class MediaRequest extends Request<MediaResponse> {
    public MediaRequest(String url) {
        super(url, false);
    }

    @Override
    public MediaResponse send() throws IOException {
        return new MediaResponse((HttpURLConnection) url.openConnection());
    }
}
