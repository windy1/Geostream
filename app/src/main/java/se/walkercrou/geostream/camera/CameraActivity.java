package se.walkercrou.geostream.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.melnykov.fab.FloatingActionButton;

import java.util.Arrays;

import se.walkercrou.geostream.LocationManager;
import se.walkercrou.geostream.Post;
import se.walkercrou.geostream.R;
import se.walkercrou.geostream.util.AppUtil;
import se.walkercrou.geostream.util.DialogUtil;

/**
 * Activity launched when you click the camera FAB in the MapsActivity. Takes pictures and video to
 * be posted.
 */
@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements Camera.PictureCallback,
        Camera.ShutterCallback, Camera.PreviewCallback {

    // camera stuff
    private static final long PROGRESS_PAUSE_TIME = 100; // 10 seconds
    private Camera cam;
    private CameraPreview preview;
    private boolean recording = false;
    private ProgressBar recordingProgress;
    private int recordingProgressStatus = 0;
    private byte[] imageData;

    // ui stuff
    private final Handler handler = new Handler();
    private View cancelBtn;
    private FloatingActionButton recordBtn, sendBtn;

    // location stuff
    private LocationManager locationManager;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        // hide action bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);

        // ui references
        cancelBtn = findViewById(R.id.btn_cancel);
        sendBtn = (FloatingActionButton) findViewById(R.id.fab_send);
        // hide the send button without an animation at start
        sendBtn.hide(false);

        // add listeners to record button
        recordBtn = (FloatingActionButton) findViewById(R.id.fab_record);
        // start recording when the user presses and holds the button
        recordBtn.setOnLongClickListener(this::startRecording);
        // take a picture when the user clicks the button
        recordBtn.setOnClickListener((view) -> cam.takePicture(this, this, this));
        // stop recording if the user lets go of the button (and we are recording)
        // if action == ACTION_UP && recording == true: stop recording
        recordBtn.setOnTouchListener((view, event) -> event.getAction() == MotionEvent.ACTION_UP
                && recording && stopRecording());

        // setup progress bar for recording video
        recordingProgress = (ProgressBar) findViewById(R.id.progress_bar);

        // connect to location services
        locationManager = new LocationManager(this);
        locationManager.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // resume the camera
        setupCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        // pause the camera
        preview.stopPreviewAndFreeCamera();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        // called twice sometimes, once with null data for some reason
        if (data == null)
            return;
        imageData = data;
        AppUtil.d("imageData = " + Arrays.toString(data));
        // display the playback buttons
        showPlaybackButtons();
    }

    @Override
    public void onShutter() {
        // called when the shutter sound is made
        // do nothing
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // called every frame while the preview is running
    }

    public void resumePreview(View view) {
        // called when the cancel button is clicked
        // resume the preview
        cam.startPreview();
        // show the normal buttons again
        showPreviewButtons();
    }

    public void sendPost(View view) {
        // called when the send button is clicked
        // check for network access
        if (AppUtil.isConnectedToNetwork(this))
            DialogUtil.connectionError(this, (dialog, which) -> sendPost(null)).show();
        else {
            // try to create post
            Post post = Post.create(locationManager.getLastLocation(), imageData,
                    (error) -> DialogUtil.sendPostError(this).show());
            // open activity if created
            if (post != null)
                post.startActivity(this);
        }
    }

    private void showPreviewButtons() {
        // hide the cancel and send buttons and show the record button
        sendBtn.hide();
        recordBtn.show();
        cancelBtn.setVisibility(View.GONE);
    }

    private void showPlaybackButtons() {
        // hide record button and show cancel and send buttons
        recordBtn.hide();
        sendBtn.show();
        cancelBtn.setVisibility(View.VISIBLE);
    }

    private void setupCamera() {
        // create the preview
        preview = new CameraPreview(this, this);

        // open the camera
        openCamera();
        preview.setCamera(cam);

        // add preview to frame layout
        FrameLayout previewView = (FrameLayout) findViewById(R.id.camera_preview);
        previewView.addView(preview);
    }

    private void openCamera() {
        // try to open the camera
        try {
            cam = Camera.open();
        } catch (Exception e) {
            // show error dialog
            DialogUtil.openCameraError(this).show();
        }
    }

    private boolean startRecording(View view) {
        recording = true;
        AppUtil.d("Recording");
        new Thread(this::startProgressBar).start();
        return true;
    }

    private boolean stopRecording() {
        recording = false;
        AppUtil.d("Done recording");
        startPlayback();
        return true;
    }

    private void startPlayback() {
        AppUtil.d("Playing back video");
        showPlaybackButtons();
    }

    private void startProgressBar() {
        // called when the record button is long pressed and stops when the button is released or
        // the maximum video length is reached
        while (recordingProgressStatus < 100 && recording) {
            // pause
            try {
                Thread.sleep(PROGRESS_PAUSE_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // update progress bar on ui thread
            handler.post(() -> recordingProgress.setProgress(++recordingProgressStatus));
        }

        // set back to zero
        recordingProgress.setProgress(recordingProgressStatus = 0);
    }
}
