package se.walkercrou.geostream.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.melnykov.fab.FloatingActionButton;

import java.util.Arrays;

import se.walkercrou.geostream.LocationServices;
import se.walkercrou.geostream.Post;
import se.walkercrou.geostream.R;
import se.walkercrou.geostream.util.AppUtil;
import se.walkercrou.geostream.util.DialogUtil;

/**
 * Activity launched when you click the camera FAB in the MapsActivity. Takes pictures and video to
 * be posted.
 */
@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements View.OnClickListener,
        View.OnLongClickListener, View.OnTouchListener, Camera.PictureCallback,
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
    private LocationServices locationServices;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);

        cancelBtn = findViewById(R.id.btn_cancel);
        sendBtn = (FloatingActionButton) findViewById(R.id.fab_send);
        sendBtn.hide(false);

        // add listeners to record button
        recordBtn = (FloatingActionButton) findViewById(R.id.fab_record);
        recordBtn.setOnClickListener(this);
        recordBtn.setOnLongClickListener(this);
        recordBtn.setOnTouchListener(this);

        // setup progress bar for recording video
        recordingProgress = (ProgressBar) findViewById(R.id.progress_bar);

        locationServices = new LocationServices(this);
        locationServices.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        preview.stopPreviewAndFreeCamera();
    }

    @Override
    public void onClick(View v) {
        AppUtil.d("Record button clicked");
        cam.takePicture(this, this, this);
    }

    @Override
    public boolean onLongClick(View v) {
        AppUtil.d("Record button long clicked");
        startRecording();
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // called when the record button is touched
        if (event.getAction() == MotionEvent.ACTION_UP && recording) {
            // record button has been released while recording
            stopRecording();
            return true;
        }
        return false;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (data == null)
            return;
        imageData = data;
        AppUtil.d("imageData = " + Arrays.toString(data));
        showPlaybackButtons();
    }

    @Override
    public void onShutter() {
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
    }

    public void resumePreview(View view) {
        // called when the cancel button is clicked
        // resume the preview
        cam.startPreview();
        showPreviewButtons();
    }

    public void sendPost(View view) {
        // called when the send button is clicked
        // check for network access
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected())
            // no network connection
            DialogUtil.connectionError(this, (dialog, which) -> sendPost(null)).show();
        else {
            Post post = Post.create(locationServices.getLastLocation(), imageData);
            if (post == null)
                DialogUtil.sendPostError(this).show();
            else
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

    private void startRecording() {
        recording = true;
        AppUtil.d("Recording");
        new Thread(this::startProgressBar).start();
    }

    private void stopRecording() {
        recording = false;
        AppUtil.d("Done recording");
        startPlayback();
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
