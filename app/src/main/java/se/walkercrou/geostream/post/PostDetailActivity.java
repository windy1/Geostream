package se.walkercrou.geostream.post;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.walkercrou.geostream.MapActivity;
import se.walkercrou.geostream.R;
import se.walkercrou.geostream.net.request.MediaRequest;
import se.walkercrou.geostream.net.response.MediaResponse;
import se.walkercrou.geostream.util.E;
import se.walkercrou.geostream.util.G;

/**
 * Activity launched when a new {@link Post} is created or a marker is clicked on the
 * {@link MapActivity}. The intent must contain a {@link #EXTRA_POST} with the {@link Post} that
 * is to be displayed. Provides back navigation to the {@link MapActivity}.
 */
@SuppressWarnings("deprecation")
public class PostDetailActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {
    /**
     * Extra that contains the Post that is expected in this activity.
     */
    public static final String EXTRA_POST = "post";
    /**
     * The ViewPager position for the {@link MediaFragment}.
     */
    public static final int PAGE_MEDIA = 0;
    /**
     * The ViewPager position for the {@link CommentsFragment}.
     */
    public static final int PAGE_COMMENTS = 1;
    /**
     * Class that handles actions related to the {@link ActionBar}.
     */
    private static ActionBarHandler actionBarHandler;
    private static VideoHandler videoHandler;

    private Post post;
    private String clientSecret; // signifies ownership of the post
    private Object media; // either a Bitmap or String file path to video file
    private ViewPager viewPager;
    private ProgressDialog progressDialog;
    private final Handler handler = new Handler();

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY); // as to not distort the media
        setContentView(R.layout.activity_post_detail);

        // get the passed post object
        post = getIntent().getParcelableExtra(EXTRA_POST);
        if (post == null)
            throw new RuntimeException("PostDetailActivity started with null Post");

        // see if we have the client secret for this post, okay if not
        clientSecret = G.app.secrets.getString(Integer.toString(post.getId()), null);
        if (clientSecret != null) {
            G.i("This device is the owner of the Post");
            G.i("  client_secret=\"" + clientSecret + "\"");
        }

        downloadMedia(); // sets 'media' to either a Bitmap or String file path

        actionBarHandler = new ActionBarHandler(getActionBar(), clientSecret);
        videoHandler = new VideoHandler();

        // setup view paging between post and comments
        PostDetailPagerAdapter adapter = new PostDetailPagerAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post_detail_activity_actions, menu);
        actionBarHandler.onMenuInflate(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                // show confirmation dialog for deletion
                showDeleteDialog();
                return true;
            case R.id.action_hide:
                showHideDialog();
                return true;
            case R.id.action_flag:
                showReportDialog();
                return true;
            case R.id.action_comments:
                // switch to comments
                viewPager.setCurrentItem(PAGE_COMMENTS);
                return true;
            case android.R.id.home:
                // if at comments, go back to media, otherwise go back to map
                if (viewPager.getCurrentItem() == PAGE_COMMENTS)
                    viewPager.setCurrentItem(PAGE_MEDIA);
                else
                    NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        actionBarHandler.onPageSelected(position);
        videoHandler.onPageSelected(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    /**
     * Hides this post and starts the {@link MapActivity}.
     */
    public void hidePost() {
        post.setHidden(true);
        startActivity(new Intent(this, MapActivity.class));
    }

    /**
     * Attempts to report this Post to the server.
     *
     * @param reason of report
     */
    public void reportPost(Flag.Reason reason) {
        progressDialog = ProgressDialog.show(this, getString(R.string.title_wait),
                getString(R.string.prompt_reporting_post), true);
        new Thread(() -> _reportPost(reason)).start();
    }

    private void _reportPost(Flag.Reason reason) {
        try {
            post.flag(this, reason, (error) -> E.internal(this, error));
        } catch (IOException e) {
            e.printStackTrace();
            E.report(this);
            return;
        }

        if (progressDialog != null)
            progressDialog.dismiss();

        post.setHidden(true);
        handler.post(() -> Toast.makeText(this, R.string.prompt_post_report, Toast.LENGTH_LONG)
                .show());
        startActivity(new Intent(this, MapActivity.class));
    }

    /**
     * Attempts to delete this Post on the server and client.
     */
    public void deletePost() {
        progressDialog = ProgressDialog.show(this, getString(R.string.title_wait),
                getString(R.string.prompt_discarding_post), true);
        new Thread(this::_deletePost).start();
    }

    private void _deletePost() {
        try {
            post.delete(this, clientSecret, (error) -> E.internal(this, error));
        } catch (IOException e) {
            e.printStackTrace();
            E.discard(this);
            return;
        }

        if (progressDialog != null)
            progressDialog.dismiss();

        startActivity(new Intent(this, MapActivity.class));
    }

    private void showHideDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_confirm)
                .setMessage(R.string.prompt_confirm_hide)
                .setPositiveButton(R.string.action_hide, (dialog, which) -> {
                    dialog.dismiss();
                    hidePost();
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_confirm)
                .setMessage(R.string.prompt_confirm_delete)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    dialog.dismiss();
                    deletePost();
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showReportDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_report)
                .setItems(R.array.report_reasons, (dialog, which) -> {
                    reportPost(Flag.Reason.values()[which]);
                })
                .show();
    }

    private void downloadMedia() {
        // TODO: Move media request handling to Post
        // download the media for this post which is either an image or video
        String url = post.getMediaUrl();
        G.i("Downloading Post media from: " + url);

        // get response from server
        MediaResponse response;
        try {
            response = new MediaRequest(this, url).sendInBackground();
        } catch (IOException e) {
            E.connection(this, (d, w) -> downloadMedia());
            return;
        }

        // check response
        if (response == null)
            throw new RuntimeException("Could not download post media");
        else if (response.isError())
            throw new RuntimeException("Could not download post media: "
                    + response.getStatusCode());

        // get and check result
        Object media = response.get();
        if (media instanceof File) {
            // received a video
            this.media = response.get().toString();
        } else {
            // received an image
            Bitmap bmp = (Bitmap) response.get();
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            this.media = Bitmap.createBitmap(
                    bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true
            );
        }
    }

    /**
     * Wrapper class for handling the ActionBar.
     */
    public static class ActionBarHandler {
        /**
         * ActionBar button to delete the Post.
         */
        public static final int ACTION_DISCARD = 0;
        /**
         * ActionBar button to hide the Post.
         */
        public static final int ACTION_HIDE = 1;
        /**
         * ActionBar button to report the Post.
         */
        public static final int ACTION_REPORT = 2;
        /**
         * ActionBar button to open the comments.
         */
        public static final int ACTION_COMMENTS = 3;
        /**
         * The delay (in millis) to wait before hiding the {@link ActionBar}.
         */
        public static final int HIDE_DELAY = 3000;

        private final ActionBar ab;
        private final String clientSecret;
        private final Map<DelayedHideTask, Boolean> hideTasks = new HashMap<>();
        private final Handler handler = new Handler();
        private MenuItem commentsItem;

        public ActionBarHandler(ActionBar ab, String clientSecret) {
            this.ab = ab;
            this.clientSecret = clientSecret;
            ab.setDisplayHomeAsUpEnabled(true);
        }

        /**
         * Called when the media is added to the {@link MediaFragment}.
         */
        public void onMediaLoad() {
            hideActionBar();
        }

        /**
         * Called when the media is clicked.
         */
        public void onMediaClick() {
            showActionBar();
            hideActionBar();
        }

        /**
         * Called when the action menu on the {@link ActionBar} is inflated.
         *
         * @param menu that was inflated
         */
        public void onMenuInflate(Menu menu) {
            commentsItem = menu.getItem(ACTION_COMMENTS);
            if (clientSecret == null)
                // hide "discard" button if we don't have a client secret
                menu.getItem(ACTION_DISCARD).setVisible(false);
        }

        /**
         * Called when {@link ViewPager} changes pages in the activity.
         *
         * @param position of page
         */
        public void onPageSelected(int position) {
            if (position == PAGE_MEDIA) {
                // switched to media fragment
                commentsItem.setVisible(true);
                hideActionBar();
            } else {
                commentsItem.setVisible(false);
                showActionBar();
            }
        }

        private void hideActionBar() {
            cancelHideTasks(); // cancel pending hide tasks
            DelayedHideTask hideTask = new DelayedHideTask();
            hideTasks.put(hideTask, false);
            hideTask.start();
        }

        private void showActionBar() {
            cancelHideTasks();
            ab.show();
        }

        private void cancelHideTasks() {
            for (DelayedHideTask hideTask : hideTasks.keySet())
                hideTasks.put(hideTask, true);
        }

        /**
         * Thread that does a delayed hide of the {@link ActionBar}.
         */
        public class DelayedHideTask extends Thread {
            @Override
            public void run() {
                try {
                    Thread.sleep(HIDE_DELAY);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                boolean cancelled = hideTasks.get(this);
                if (!cancelled)
                    handler.post(ab::hide);

                hideTasks.remove(this);
            }
        }
    }

    /**
     * Wrapper class for handling video playback.
     */
    public static class VideoHandler {
        private MediaPlayer mediaPlayer;

        /**
         * Called when the video is clicked. Pauses if playing and plays if paused.
         */
        public void onVideoClick() {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying())
                    mediaPlayer.pause();
                else
                    mediaPlayer.start();
            }
        }

        /**
         * Called when the page is changed by the {@link ViewPager}. Starts the {@link MediaPlayer}
         * if switched to the {@link MediaFragment} and pauses if switched to the
         * {@link CommentsFragment}.
         *
         * @param position of page
         */
        public void onPageSelected(int position) {
            if (position == PAGE_MEDIA) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying())
                    mediaPlayer.start();
            } else if (mediaPlayer != null && mediaPlayer.isPlaying())
                mediaPlayer.pause();
        }
    }

    /**
     * Handles navigation between the {@link MediaFragment} and the {@link CommentsFragment}.
     */
    public class PostDetailPagerAdapter extends FragmentPagerAdapter {
        /**
         * The total number of pages in the {@link ViewPager}.
         */
        public static final int NUM_PAGES = 2;

        public PostDetailPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    // prepare arguments
                    Bundle args = new Bundle();
                    args.putParcelable(MediaFragment.ARG_POST, post);
                    if (media instanceof String) {
                        // video
                        args.putString(MediaFragment.ARG_VIDEO_FILE, media.toString());
                    } else {
                        // image
                        args.putParcelable(MediaFragment.ARG_IMAGE, (Bitmap) media);
                    }

                    // create fragment
                    MediaFragment frag = new MediaFragment();
                    frag.setArguments(args);
                    return frag;
                case 1:
                    CommentsFragment comments = new CommentsFragment();
                    Bundle cargs = new Bundle();
                    cargs.putParcelable(CommentsFragment.ARG_POST, post);
                    comments.setArguments(cargs);
                    return comments;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    /**
     * Represents the tab that shows the media of the post.
     */
    public static class MediaFragment extends Fragment implements View.OnClickListener {
        /**
         * {@link Bitmap}: Media image of post. Passed only in the absence of
         * {@link #ARG_VIDEO_FILE}.
         */
        public static final String ARG_IMAGE = "image";
        /**
         * {@link String}: File path to video file of post. Passed only in the absence of
         * {@link #ARG_IMAGE}.
         */
        public static final String ARG_VIDEO_FILE = "video_file";
        /**
         * {@link Post}: The post of the activity.
         */
        public static final String ARG_POST = "post";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            View view = inflater.inflate(R.layout.fragment_media, container, false);
            Bundle args = getArguments();

            // initialize media
            Bitmap bmp = args.getParcelable(ARG_IMAGE);
            FrameLayout fl = (FrameLayout) view.findViewById(R.id.media);
            fl.setOnClickListener(this);

            if (bmp == null) {
                // not an image, check for video
                String videoFilePath = args.getString(ARG_VIDEO_FILE);
                if (videoFilePath == null)
                    throw new RuntimeException("no image or video passed to MediaFragment");

                // add video to view
                VideoPreview video = new VideoPreview(getContext(), videoFilePath);
                fl.addView(video);

                videoHandler.mediaPlayer = video.player;
            } else {
                // image found, add to view
                ImageView image = new ImageView(getContext());
                image.setImageBitmap(bmp);
                fl.addView(image);
            }

            actionBarHandler.onMediaLoad();

            // set the time
            Post post = args.getParcelable(ARG_POST);
            if (post != null)
                ((TextView) view.findViewById(R.id.created))
                        .setText(G.getTimeDisplay(post.getCreationDate()));

            return view;
        }

        @Override
        public void onClick(View v) {
            actionBarHandler.onMediaClick();
            videoHandler.onVideoClick();
        }
    }

    /**
     * Custom {@link ArrayAdapter} to handle Comment inflation within the ListView
     */
    public static class CommentAdapter extends ArrayAdapter<Comment> {
        public CommentAdapter(Context context, List<Comment> comments) {
            super(context, R.layout.list_item_comment, R.id.content, comments);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            // set the date, the comment content is set automatically in the super class using
            // Comment.toString()
            Date date = getItem(position).getCreationDate();
            ((TextView) v.findViewById(R.id.created)).setText(G.getTimeDisplay(date));
            return v;
        }
    }

    /**
     * Represents the comments section.
     */
    public static class CommentsFragment extends Fragment implements View.OnClickListener,
            SwipeRefreshLayout.OnRefreshListener {
        /**
         * {@link Post}: The post of the activity.
         */
        public static final String ARG_POST = "post";

        private View view;
        private CommentAdapter adapter;
        private Post post;
        private SwipeRefreshLayout swipeLayout; // ListView container that handles refreshing

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            // inflate view
            View view = inflater.inflate(R.layout.fragment_comments, container, false);
            view.findViewById(R.id.btn_send).setOnClickListener(this);

            // get post object
            post = getArguments().getParcelable(ARG_POST);
            if (post == null)
                throw new RuntimeException();

            // add comments to list view
            adapter = new CommentAdapter(getContext(), new ArrayList<>(post.getComments()));
            ListView listView = (ListView) view.findViewById(R.id.list_comments);
            listView.setAdapter(adapter);

            // initialize refresh
            swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
            swipeLayout.setOnRefreshListener(this);
            swipeLayout.setColorSchemeColors(android.R.color.holo_blue_dark);

            return this.view = view;
        }

        @Override
        public void onClick(View btn) {
            // called when the "reply" button is clicked
            // get comment content from view
            EditText field = (EditText) view.findViewById(R.id.text_comment);
            String content = field.getText().toString().trim();

            // make sure comment has content
            if (content.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.error_empty_reply),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // create comment
            Comment comment;
            try {
                comment = post.comment(getContext(), content, G::e);
            } catch (IOException e) {
                E.comment(getContext()).show();
                return;
            }

            // add comment to ListView
            if (comment != null) {
                G.d(comment.getContent());
                adapter.add(comment);
            }

            // clear and remove focus from reply box
            field.setText("", TextView.BufferType.EDITABLE);
            view.requestFocus();
            hideKeyboard();
        }

        private void hideKeyboard() {
            // hides the virtual keyboard
            InputMethodManager in = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
        }

        @Override
        public void onRefresh() {
            // retrieve new comments from the server and update the adapter
            Context c = getContext();
            new Handler().post(() -> {
                try {
                    post.refreshComments(c, G::e);
                } catch (IOException e) {
                    Toast.makeText(c, c.getString(R.string.error_refresh_comments),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                adapter.clear();
                adapter.addAll(post.getComments());
                swipeLayout.setRefreshing(false);
            });
        }
    }
}
