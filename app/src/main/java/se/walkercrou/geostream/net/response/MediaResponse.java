package se.walkercrou.geostream.net.response;

import android.content.Context;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import se.walkercrou.geostream.post.Post;

public class MediaResponse extends Response<Object> {
    private final Object media;

    public MediaResponse(Context c, boolean video, InputStream in, int statusCode,
                         String statusMessage) {
        super(in, statusCode, statusMessage);

        if (video) {
            FileOutputStream out = null;
            try {
                // create file for video
                File file = new File(c.getExternalCacheDir(), Post.fileName(true));
                if (!file.exists())
                    file.createNewFile();

                // write input stream to file
                out = new FileOutputStream(file);
                byte[] buffer = new byte[4 * 1024];
                int n;
                while ((n = in.read(buffer)) != -1)
                    out.write(buffer, 0, n);

                media = file;
            } catch (IOException e) {
                throw new RuntimeException("error while downloading media video : ", e);
            } finally {
                if (out != null)
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        } else
            // not a video, decode bitmap
            media = BitmapFactory.decodeStream(in);

        // close input stream
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MediaResponse(Context c, boolean video, HttpURLConnection conn) throws IOException {
        this(c, video, conn.getInputStream(), conn.getResponseCode(), conn.getResponseMessage());
    }

    @Override
    public Object get() {
        return media;
    }
}
