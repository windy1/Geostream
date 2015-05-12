package se.walkercrou.geostream.net.response;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A server's response for a media request.
 */
public class MediaResponse extends Response<Bitmap> {
    private Bitmap bmp;

    public MediaResponse(int statusCode, String statusMessage, InputStream in) {
        super(statusCode, statusMessage, in);

        // read the byte data
        List<Byte> data = new ArrayList<>();
        try {
            int b;
            while ((b = in.read()) != -1)
                data.add((byte) b);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // decode to bitmap
        byte[] dataArray = new byte[data.size()];
        for (int i = 0; i < dataArray.length; i++)
            dataArray[i] = data.get(i);
        bmp = BitmapFactory.decodeByteArray(dataArray, 0, dataArray.length);

        // rotate bitmap 90deg right
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth(), bmp.getHeight(), true);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
    }

    @Override
    public Bitmap get() {
        return bmp;
    }
}
