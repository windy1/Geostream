package se.walkercrou.geostream.net.response;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class MediaResponse extends Response<Bitmap> {
    private final Bitmap bmp;

    public MediaResponse(InputStream in, int statusCode, String statusMessage) {
        super(in, statusCode, statusMessage);
        bmp = BitmapFactory.decodeStream(in);
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MediaResponse(HttpURLConnection conn) throws IOException {
        this(conn.getInputStream(), conn.getResponseCode(), conn.getResponseMessage());
    }

    @Override
    public Bitmap get() {
        return bmp;
    }
}
