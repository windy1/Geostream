package se.walkercrou.geostream.post;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import se.walkercrou.geostream.util.G;

@SuppressWarnings("deprecation")
public class VideoPreview extends SurfaceView implements SurfaceHolder.Callback {
    private final MediaPlayer player = new MediaPlayer();
    private final String dataSource;

    public VideoPreview(Context context, String dataSource) {
        super(context);
        this.dataSource = dataSource;

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        player.setDisplay(holder);
        player.setScreenOnWhilePlaying(true);
        player.setLooping(true);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            player.setDataSource(dataSource);
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        player.stop();
        player.release();
    }
}
