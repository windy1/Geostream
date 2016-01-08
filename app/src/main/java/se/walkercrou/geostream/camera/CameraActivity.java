package se.walkercrou.geostream.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.io.IOException;

import se.walkercrou.geostream.MapActivity;
import se.walkercrou.geostream.R;
import se.walkercrou.geostream.post.Post;
import se.walkercrou.geostream.post.PostDetailActivity;
import se.walkercrou.geostream.util.E;
import se.walkercrou.geostream.util.G;
import se.walkercrou.geostream.util.LocationApi;

import static android.hardware.Camera.PictureCallback;
import static android.hardware.Camera.PreviewCallback;
import static android.hardware.Camera.ShutterCallback;
import static android.hardware.Camera.open;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import static se.walkercrou.geostream.net.request.ResourceCreateRequest.MediaData;

/**
 * Activity launched when you click the camera button in the {@link MapActivity}. Takes pictures and
 * videos to be posted. Provides back navigation to the {@link MapActivity} and opens a
 * {@link PostDetailActivity} when a new post is created.
 */
@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements PictureCallback, ShutterCallback,
        PreviewCallback, ConnectionCallbacks, OnConnectionFailedListener {
    private static final long PROGRESS_PAUSE_TIME = 100; // 10 seconds, used for recording anim

    // camera stuff
    private Camera cam;
    private CameraPreview preview; // SurfaceView that is attached to FrameLayout
    private final MediaRecorder recorder = new MediaRecorder();
    private boolean recording = false;
    private File outputFile; // video output file
    private byte[] imageData;

    // ui stuff
    private FrameLayout previewView; // the view that the CameraPreview is attached to
    private ProgressBar recordingProgress;
    private int recordingProgressStatus = 0;
    private final Handler handler = new Handler(); // used for posting ui updates for progress bar
    private ProgressDialog progressDialog;
    private View cancelBtn;
    private FloatingActionButton recordBtn, sendBtn;
    private View flashOnBtn, flashOffBtn;
    private boolean flashOn = false;
    private View cameraFrontBtn, cameraRearBtn;
    private boolean cameraFacingFront = false;

    // location stuff
    private LocationApi locationApi;
    private boolean lostLocation;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        // hide action bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);

        // ui references
        cancelBtn = findViewById(R.id.btn_cancel);
        sendBtn = (FloatingActionButton) findViewById(R.id.fab_send);
        sendBtn.hide(false); // hide the send button without an animation at start

        flashOnBtn = findViewById(R.id.btn_flash_on);
        flashOffBtn = findViewById(R.id.btn_flash_off);

        cameraFrontBtn = findViewById(R.id.btn_camera_front);
        cameraRearBtn = findViewById(R.id.btn_camera_rear);

        setupRecordButton();

        // setup progress bar for recording video
        recordingProgress = (ProgressBar) findViewById(R.id.progress_bar);

        previewView = (FrameLayout) findViewById(R.id.camera_preview);
    }

    @Override
    protected void onStart() {
        locationApi = new LocationApi(this);
        locationApi.connect(this, this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        locationApi.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // resume the camera
        setupCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // pause the camera
        preview.stopPreviewAndFreeCamera();
    }

    @Override
    public void onConnected(Bundle bundle) {
        G.i("Connected to location services.");
        if (lostLocation) {
            Toast.makeText(this, R.string.prompt_location_restored, Toast.LENGTH_LONG).show();
            lostLocation = false;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        G.i("Connection to location services lost.");
        Toast.makeText(this, R.string.error_lost_connection, Toast.LENGTH_LONG).show();
        lostLocation = true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        G.i("Connection to location services failed.");
        E.location(this, (d, w) -> locationApi.connect(this, this)).show();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        // called twice sometimes, once with null data for some reason
        if (data == null)
            return;
        G.i("Image captured.");
        imageData = data;
        showPlaybackButtons(); // display the playback buttons
    }

    @Override
    public void onShutter() {
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
    }

    // -- Methods called by XML --

    public void resumePreview(View view) {
        if (outputFile != null && outputFile.exists()) {
            // video file exists
            preview.stopPlayback();
            outputFile.delete();
            outputFile = null;
            setupCamera(); // reset camera
        } else {
            // image data exists
            cam.startPreview();
            imageData = null;
        }
        showPreviewButtons();
    }

    public void sendPost(View view) {
        G.i("Sending post to server.");
        // called when the send button is clicked
        // check for network access
        if (!G.isConnectedToNetwork(this))
            E.connection(this, (dialog, which) -> sendPost(null)).show();
        else {
            // start progress dialog
            preview.stopPlayback();

            // configure number picker
            LayoutInflater inflater = LayoutInflater.from(this);
            NumberPicker np = (NumberPicker) inflater.inflate(R.layout.number_picker, null);
            np.setMaxValue(24);
            np.setMinValue(1);
            np.setValue(24);
            np.setWrapSelectorWheel(false);

            // show number picker dialog
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_lifetime)
                    .setMessage(R.string.prompt_lifetime)
                    .setView(np)
                    .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                        // send the post
                        dialog.dismiss();
                        progressDialog = ProgressDialog.show(this, getString(R.string.title_wait),
                                getString(R.string.prompt_creating_post), true);
                        new Thread(() -> sendPost(np.getValue())).start();
                    })
                    .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    public void toggleFlash(View view) {
        flashOn = !flashOn;
        showFlashButton();
    }

    public void switchCameraDirection(View view) {
        cameraFacingFront = !cameraFacingFront;
        showSwitchCameraButton();
    }

    // ---------------------------

    private void setupRecordButton() {
        // add listeners to record button
        recordBtn = (FloatingActionButton) findViewById(R.id.fab_record);
        // start recording video when the user presses and holds the button
        recordBtn.setOnLongClickListener(this::startRecording);
        // take a picture when the user clicks the button
        recordBtn.setOnClickListener((view) -> cam.takePicture(this, this, this));
        // stop recording if the user lets go of the button (and we are recording)
        // if action == ACTION_UP && recording == true: stop recording
        recordBtn.setOnTouchListener((view, event) -> event.getAction() == MotionEvent.ACTION_UP
                && recording && stopRecording());
    }

    private void sendPost(int lifetime) {
        // construct MediaData object
        MediaData data;
        if (outputFile != null && outputFile.exists())
            data = new MediaData(Post.fileName(true), outputFile);
        else
            data = new MediaData(Post.fileName(false), imageData);

        // try to create post
        Post post;
        try {
            post = Post.create(this, locationApi.getLastLocation(), lifetime, data,
                    (error) -> E.postSend(this).show());
        } catch (IOException e) {
            E.postSend(this).show();
            e.printStackTrace();
            return;
        }

        if (post != null)
            post.startActivity(this);

        if (progressDialog != null)
            progressDialog.dismiss();
    }

    private void showPreviewButtons() {
        // hide the cancel and send buttons and show the record button
        sendBtn.hide();
        recordBtn.show();
        cancelBtn.setVisibility(View.GONE);
        showFlashButton();
        showSwitchCameraButton();
    }

    private void showFlashButton() {
        if (flashOn) {
            flashOnBtn.setVisibility(View.VISIBLE);
            flashOffBtn.setVisibility(View.INVISIBLE);
        } else {
            flashOffBtn.setVisibility(View.VISIBLE);
            flashOnBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void showSwitchCameraButton() {
        if (cameraFacingFront) {
            cameraFrontBtn.setVisibility(View.VISIBLE);
            cameraRearBtn.setVisibility(View.INVISIBLE);
        } else {
            cameraRearBtn.setVisibility(View.VISIBLE);
            cameraFrontBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void showPlaybackButtons() {
        // hide record button and show cancel and send buttons
        recordBtn.setPressed(false);
        recordBtn.hide();
        sendBtn.show();
        cancelBtn.setVisibility(View.VISIBLE);
        flashOnBtn.setVisibility(View.INVISIBLE);
        flashOffBtn.setVisibility(View.INVISIBLE);
        cameraFrontBtn.setVisibility(View.INVISIBLE);
        cameraRearBtn.setVisibility(View.INVISIBLE);
    }

    private void setupCamera() {
        // create the preview
        preview = new CameraPreview(this, this);

        // open the camera
        openCamera();
        cam.getParameters().setRecordingHint(true);
        preview.setCamera(cam);

        // add preview to frame layout
        previewView.removeAllViews();
        previewView.addView(preview);

        G.i("Camera initialized.");
    }

    private void openCamera() {
        // try to open the camera
        try {
            cam = open();
        } catch (Exception e) {
            throw new RuntimeException("could not open camera : ", e);
        }
    }

    private boolean startRecording(View view) {
        G.d("startRecording(View)");
        recording = true;
        new Thread(this::startProgressBar).start(); // start recording progress bar in background

        // create video output file
        outputFile = new File(getExternalCacheDir(), Post.fileName(true));
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("could not create media output file : ", e);
        }

        // configure recorder
        cam.unlock();
        recorder.setCamera(cam);
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        recorder.setOutputFile(outputFile.toString());
        recorder.setPreviewDisplay(preview.holder.getSurface());
        recorder.setOrientationHint(90);

        try {
            recorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException("failed to prepare to record video : ", e);
        }

        recorder.start();

        G.i("Recording video.");

        return true;
    }

    private boolean stopRecording() {
        recording = false;

        // stop recorder and reset for future use
        recorder.stop();
        recorder.reset();
        cam.lock();

        G.i("Recording complete. Starting playback.");

        startPlayback();
        return true;
    }

    private void startPlayback() {
        showPlaybackButtons();
        preview.startPlayback(outputFile.toString());
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
        if (recording)
            handler.post(this::stopRecording); // force stop recording
    }
}
