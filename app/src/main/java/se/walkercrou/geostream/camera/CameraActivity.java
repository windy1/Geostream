package se.walkercrou.geostream.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import se.walkercrou.geostream.R;
import se.walkercrou.geostream.post.Post;
import se.walkercrou.geostream.util.E;
import se.walkercrou.geostream.util.G;
import se.walkercrou.geostream.util.LocationManager;

import static android.hardware.Camera.PictureCallback;
import static android.hardware.Camera.PreviewCallback;
import static android.hardware.Camera.ShutterCallback;
import static android.hardware.Camera.open;
import static se.walkercrou.geostream.net.request.ResourceCreateRequest.MediaData;

/**
 * Activity launched when you click the camera FAB in the MapsActivity. Takes pictures and video to
 * be posted.
 *
 * TODO: Record video
 */
@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements PictureCallback, ShutterCallback,
        PreviewCallback {

    private static final long PROGRESS_PAUSE_TIME = 100; // 10 seconds, used for recording anim

    // camera stuff
    private Camera cam;
    private CameraPreview preview;
    private final MediaRecorder recorder = new MediaRecorder();
    private File outputFile; // video output file
    private boolean recording = false;
    private byte[] imageData;

    // ui stuff
    private ProgressBar recordingProgress;
    private int recordingProgressStatus = 0;
    private final Handler handler = new Handler(); // used for posting ui updates for progress bar
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
        // start recording video when the user presses and holds the button
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
        G.d("imageData = " + Arrays.toString(data));
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

    // -- Methods called by XML --

    public void resumePreview(View view) {
        // called when the cancel button is clicked
        cam.startPreview(); // resume the preview
        showPreviewButtons(); // show the normal buttons again
        imageData = null; // reset image data

        // delete outputFile if exists
        if (outputFile.exists()) {
            outputFile.delete();
            outputFile = null;
        }
    }

    public void sendPost(View view) {
        // called when the send button is clicked
        // check for network access
        if (!G.isConnectedToNetwork(this))
            E.connection(this, (dialog, which) -> sendPost(null)).show();
        else {
            // construct MediaData object
            MediaData data;
            if (outputFile != null && outputFile.exists())
                data = new MediaData(Post.fileName(true), outputFile);
            else
                data = new MediaData(Post.fileName(false), imageData);

            // try to create post
            Post post = Post.create(locationManager.getLastLocation(), data,
                    (error) -> E.postSend(this).show());
            // open activity if created
            if (post != null)
                post.startActivity(this);
        }
    }

    // ---------------------------

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
        cam.getParameters().setRecordingHint(true);
        preview.setCamera(cam);

        // add preview to frame layout
        FrameLayout previewView = (FrameLayout) findViewById(R.id.camera_preview);
        previewView.addView(preview);
    }

    private void openCamera() {
        // try to open the camera
        try {
            cam = open();
        } catch (Exception e) {
            // show error dialog
            E.cameraOpen(this).show();
        }
    }

    private boolean startRecording(View view) {
        recording = true;
        G.d("Recording");
        new Thread(this::startProgressBar).start();

        // create output file
        outputFile = new File(getExternalCacheDir(), Post.fileName(true));
        G.d("outputFile = " + outputFile);
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

        try {
            recorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException("failed to prepare to record video : ", e);
        }

        recorder.start();

        return true;
    }

    private boolean stopRecording() {
        recording = false;
        G.d("Done recording");

        // stop recorder and reset for future use
        recorder.stop();
        recorder.reset();
        recorder.release();
        cam.lock();

        startPlayback();
        return true;
    }

    private void startPlayback() {
        G.d("Playing back video");
        showPlaybackButtons();

        try {
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(outputFile.toString());
            player.setAudioStreamType(AudioManager.STREAM_SYSTEM);
            player.setDisplay(preview.holder);
            player.prepare();
            player.start();
        } catch (IOException e) {
            throw new RuntimeException("error starting video playback : ", e);
        }
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
