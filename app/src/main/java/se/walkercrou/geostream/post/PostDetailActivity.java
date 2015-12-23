package se.walkercrou.geostream.post;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import se.walkercrou.geostream.MapActivity;
import se.walkercrou.geostream.R;
import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.request.MediaRequest;
import se.walkercrou.geostream.net.request.ResourceDeleteRequest;
import se.walkercrou.geostream.net.response.MediaResponse;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.E;
import se.walkercrou.geostream.util.G;

import static android.app.ActionBar.*;
import static android.support.v4.view.ViewPager.*;

/**
 * Represents an activity that displays a post's details.
 */
@SuppressWarnings("deprecation")
public class PostDetailActivity extends FragmentActivity implements TabListener,
        OnPageChangeListener {
    /**
     * Extra that contains the Post that is expected in this activity.
     */
    public static final String EXTRA_POST = "post";

    private Post post;
    private String clientSecret;
    private ViewPager viewPager;
    private Bitmap media;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_post_detail);

        // get the passed post object
        post = getIntent().getParcelableExtra(EXTRA_POST);
        if (post == null)
            throw new RuntimeException("PostDetailActivity started with null Post");

        // see if we have the client secret for this post, okay if not
        clientSecret = G.app.secrets.getString(Integer.toString(post.getId()), null);

        // get bitmap from server
        downloadMedia();

        // setup view paging between post and comments
        PostDetailAdapter adapter = new PostDetailAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(adapter);
        viewPager.setOnPageChangeListener(this);

        setupActionBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // show "discard" button if we have the client secret for this post
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_detail_activity_actions, menu);
        if (clientSecret != null) {
            G.d(clientSecret);
            menu.getItem(0).setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        // update view pager when tab is selected
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        // update tabs when view pager is used
        ActionBar ab = getActionBar();
        if (ab != null)
            ab.setSelectedNavigationItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_discard:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_confirm)
                        .setMessage(R.string.prompt_confirm_delete)
                        .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                            dialog.dismiss();
                            try {
                                discard();
                            } catch (IOException e) {
                                E.discard(this).show();
                            }
                        })
                        .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
            default:
                return false;
        }
    }

    /**
     * Attempts to delete this Post on the server and client.
     */
    public void discard() throws IOException {
        G.d("client secret = " + clientSecret);
        ResourceResponse<Post> response = new ResourceDeleteRequest<>(this, Post.class,
                Resource.POSTS, post.getId(), clientSecret).sendInBackground(this);
        if (response == null)
            throw new RuntimeException("Could not discard post");
        else if (response.isError())
            throw new RuntimeException("Could not discard post: " + response.getErrorDetail());
        startActivity(new Intent(this, MapActivity.class));
    }

    /**
     * Returns the string to display for timestamps on Posts or Comments. Compared to the current
     * date.
     *
     * @param date to get difference of
     * @return display string
     */
    public static String getTimeDisplay(Date date) {
        Date now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
        long diff = now.getTime() - date.getTime();

        // display "<1m" if in the seconds
        long seconds = diff / 1000;
        if (seconds < 60)
            return "<1m";

        // display minutes if >1m but <1h
        long minutes = seconds / 60;
        if (minutes < 60)
            return minutes + "m";

        // display hours if >1h but <1d
        long hours = minutes / 60;
        if (hours < 24)
            return hours + "h";

        // otherwise display days
        return (hours / 24) + "d";
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        if (bar == null)
            return;

        // enable up navigation
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab().setText(R.string.title_activity_post).setTabListener(this));
        bar.addTab(bar.newTab().setText(R.string.title_comments).setTabListener(this));
    }

    private void downloadMedia() {
        String url = post.getMediaUrl();
        G.i("Downloading Post media from: " + url);
        MediaResponse response;
        try {
            response = new MediaRequest(url).sendInBackground(this);
        } catch (IOException e) {
            E.connection(this, (d, w) -> downloadMedia());
            return;
        }

        if (response == null)
            throw new RuntimeException("Could not download post media");
        else if (response.isError())
            throw new RuntimeException("Could not download post media: "
                    + response.getStatusCode());


        // rotate bitmap
        Bitmap bmp = response.get();
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        media = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
    }

    /**
     * Handles tab-to-tab navigation.
     */
    public class PostDetailAdapter extends FragmentPagerAdapter {
        public static final int NUM_PAGES = 2;

        public PostDetailAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    MediaFragment frag = new MediaFragment();
                    Bundle args = new Bundle();
                    args.putParcelable(MediaFragment.ARG_MEDIA, media);
                    args.putParcelable(MediaFragment.ARG_POST, post);
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
    public static class MediaFragment extends Fragment {
        public static final String ARG_MEDIA = "media";
        public static final String ARG_POST = "post";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            View view = inflater.inflate(R.layout.fragment_media, container, false);
            Bundle args = getArguments();

            // set the image
            FrameLayout fl = (FrameLayout) view.findViewById(R.id.media);
            ImageView image = new ImageView(getContext());
            image.setImageBitmap(args.getParcelable(ARG_MEDIA));
            fl.addView(image);

            // set the time
            Post post = args.getParcelable(ARG_POST);
            if (post != null)
                ((TextView) view.findViewById(R.id.created))
                        .setText(getTimeDisplay(post.getCreationDate()));

            return view;
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
            ((TextView) v.findViewById(R.id.created)).setText(getTimeDisplay(date));
            return v;
        }
    }

    /**
     * Represents the tab with the comments section
     */
    public static class CommentsFragment extends Fragment implements View.OnClickListener,
            SwipeRefreshLayout.OnRefreshListener {
        public static final String ARG_POST = "post";
        private View view;
        private CommentAdapter adapter;
        private Post post;
        private SwipeRefreshLayout swipeLayout;

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
            List<Comment> comments = post.getComments();
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
            Bundle args = getArguments();

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
