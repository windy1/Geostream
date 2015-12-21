package se.walkercrou.geostream.camera;

import android.content.Context;
import android.hardware.Camera;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;

/**
 * View to attach to the CameraActivity's FrameLayout for Camera implementation.
 */
@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private Camera cam;
    private final Camera.PreviewCallback callback;
    protected final SurfaceHolder holder;

    public CameraPreview(Context context, Camera.PreviewCallback callback) {
        super(context);
        this.callback = callback;

        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * Sets the Camera that is sending frames to the preview.
     *
     * @param cam to set
     */
    public void setCamera(Camera cam) {
        stopPreviewAndFreeCamera();
        this.cam = cam;
        if (cam != null) {
            try {
                cam.setPreviewDisplay(holder);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cam.startPreview();
        }
    }

    public void stopPreviewAndFreeCamera() {
        // cleans up camera
        if (cam != null) {
            cam.setPreviewCallback(null);
            getHolder().removeCallback(this);
            cam.stopPreview();
            cam.release();
            cam = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            cam.setPreviewDisplay(holder);
            cam.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null)
            return;

        try {
            cam.stopPreview();
        } catch (Exception ignored) {
        }

        // make sure the orientation is correct
        correctCameraOrientation(width, height);

        try {
            cam.setPreviewDisplay(holder);
            cam.setPreviewCallback(callback);
            cam.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreviewAndFreeCamera();
    }

    private void correctCameraOrientation(int width, int height) {
        Camera.Parameters params = cam.getParameters();
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        int rotation = display.getRotation();
        if (rotation == Surface.ROTATION_0) {
            params.setPreviewSize(height, width);
            cam.setDisplayOrientation(90);
        } else if (rotation == Surface.ROTATION_90)
            params.setPreviewSize(width, height);
        else if (rotation == Surface.ROTATION_180)
            params.setPreviewSize(height, width);
        else if (rotation == Surface.ROTATION_270) {
            params.setPreviewSize(width, height);
            cam.setDisplayOrientation(180);
        }
    }
}
