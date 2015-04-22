package se.walkercrou.geostream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import se.walkercrou.geostream.net.Post;
import se.walkercrou.geostream.util.App;

/**
 * Activity launched when you click the camera FAB in the MapsActivity. Takes pictures and video to
 * be posted.
 */
@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements View.OnClickListener,
        View.OnLongClickListener, View.OnTouchListener, Camera.PictureCallback,
        Camera.ShutterCallback, Camera.PreviewCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // camera stuff
    private static final long PROGRESS_PAUSE_TIME = 100; // 10 seconds
    private Camera cam;
    private boolean recording = false;
    private ProgressBar recordingProgress;
    private int recordingProgressStatus = 0;
    private byte[] imageData;
    private final List<byte[]> videoFrames = new ArrayList<>();

    // ui stuff
    private final Handler handler = new Handler();
    private View cancelBtn, recordBtn, sendBtn;

    // location stuff
    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);

        // connect to location api
        googleApiClient = App.buildGoogleApiClient(this, this, this);
        googleApiClient.connect();

        setupCamera();

        cancelBtn = findViewById(R.id.btn_cancel);
        sendBtn = findViewById(R.id.fab_send);

        // add listeners to record button
        recordBtn = findViewById(R.id.fab_record);
        recordBtn.setOnClickListener(this);
        recordBtn.setOnLongClickListener(this);
        recordBtn.setOnTouchListener(this);

        // setup progress bar for recording video
        recordingProgress = (ProgressBar) findViewById(R.id.progress_bar);
    }

    @Override
    public void onClick(View v) {
        App.d("Record button clicked");
        cam.takePicture(this, this, this);
    }

    @Override
    public boolean onLongClick(View v) {
        App.d("Record button long clicked");
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
        App.d(Arrays.toString(data));
        showPlaybackButtons();
    }

    @Override
    public void onShutter() {
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // called every frame while the preview is running
        if (recording)
            videoFrames.add(data);
    }

    @Override
    public void onConnected(Bundle bundle) {
        // connected to location api
        App.d("Connected to location API");
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        App.d(String.format("Last location : %s,%s",
                lastLocation.getLatitude(), lastLocation.getLongitude()));
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    public void resumePreview(View view) {
        // called when the cancel button is clicked
        showPreviewButtons();
        // resume the preview
        cam.startPreview();
    }

    public void sendPost(View view) {
        // called when the send button is clicked
        // check for network access
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected())
            return;

        // send post
        new Post(lastLocation, imageData).sendInBackground();
    }

    private void showPreviewButtons() {
        // hide the cancel and send buttons and show the record button
        cancelBtn.setVisibility(View.GONE);
        sendBtn.setVisibility(View.GONE);
        recordBtn.setVisibility(View.VISIBLE);
    }

    private void showPlaybackButtons() {
        // hide record button and show cancel and send buttons
        recordBtn.setVisibility(View.GONE);
        cancelBtn.setVisibility(View.VISIBLE);
        sendBtn.setVisibility(View.VISIBLE);
    }

    private void setupCamera() {
        // try to open the camera
        try {
            cam = Camera.open();
        } catch (Exception e) {
            // show error dialog
            showNoCameraErrorDialog();
        }

        // add CameraPreview to the FrameLayout in our xml layout
        CameraPreview preview = new CameraPreview(this, this, cam);
        FrameLayout previewView = (FrameLayout) findViewById(R.id.camera_preview);
        previewView.addView(preview);
    }

    private void showNoCameraErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String msg = String.format(getString(R.string.error_no_camera),
                getString(R.string.app_name));
        builder.setMessage(msg).setTitle(R.string.error);
        builder.setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).create().show();
    }

    private void startRecording() {
        // clear any remaining video
        videoFrames.clear();
        recording = true;
        App.d("Recording");
        new Thread(new Runnable() {
            @Override
            public void run() {
                startProgressBar();
            }
        }).start();
    }

    private void stopRecording() {
        recording = false;
        App.d("Done recording");
        startPlayback();
    }

    private void startPlayback() {
        App.d("Playing back video");
        showPlaybackButtons();
    }

    private void startProgressBar() {
        while (recordingProgressStatus < 100 && recording) {
            // pause
            try {
                Thread.sleep(PROGRESS_PAUSE_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // update progress bar on ui thread
            handler.post(new Runnable() {
                @Override
                public void run() {
                    recordingProgress.setProgress(++recordingProgressStatus);
                }
            });
        }

        // set back to zero
        recordingProgress.setProgress(recordingProgressStatus = 0);
    }
}
