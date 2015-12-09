package se.walkercrou.geostream.post;

import android.app.ActionBar;
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
import android.support.v7.internal.view.ContextThemeWrapper;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import se.walkercrou.geostream.MapActivity;
import se.walkercrou.geostream.R;
import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.request.MediaRequest;
import se.walkercrou.geostream.net.request.ResourceDeleteRequest;
import se.walkercrou.geostream.net.response.MediaResponse;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents an activity that displays a post's details.
 */
@SuppressWarnings("deprecation")
public class PostDetailActivity extends FragmentActivity implements ActionBar.TabListener,
        ViewPager.OnPageChangeListener {
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
        clientSecret = G.app.secrets.getString(Integer.toString(post.getId()), null);

        // get bitmap from server
        media = downloadMedia();

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
        if (clientSecret != null)
            menu.getItem(0).setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // update view pager when tab is selected
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
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

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_discard:
                discard();
                return true;
            default:
                return false;
        }
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        if (bar == null)
            return;

        // enable up navigation
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab().setText(R.string.post).setTabListener(this));
        bar.addTab(bar.newTab().setText(R.string.comments).setTabListener(this));
    }

    private void discard() {
        G.d("client secret = " + clientSecret);
        ResourceResponse<Post> response = new ResourceDeleteRequest<>(Post.class, Resource.POSTS,
                post.getId(), clientSecret).sendInBackground();
        if (response == null)
            throw new RuntimeException("Could not discard post");
        else if (response.isError())
            throw new RuntimeException("Could not discard post: " + response.getErrorDetail());
        startActivity(new Intent(this, MapActivity.class));
    }

    private Bitmap downloadMedia() {
        MediaResponse response = new MediaRequest(post.getFileUrl()).sendInBackground();
        if (response == null)
            throw new RuntimeException("Could not download post media");
        else if (response.isError())
            throw new RuntimeException("Could not download post media: "
                    + response.getStatusCode());


        // rotate bitmap
        Bitmap bmp = response.get();
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            View view = inflater.inflate(R.layout.fragment_media, container, false);
            Bundle args = getArguments();
            ((ImageView) view.findViewById(R.id.media))
                    .setImageBitmap(args.getParcelable(ARG_MEDIA));
            return view;
        }
    }

    /**
     * Represents the tab with the comments section
     */
    public static class CommentsFragment extends Fragment implements View.OnClickListener,
            SwipeRefreshLayout.OnRefreshListener {
        public static final String ARG_POST = "post";
        private View view;
        private ArrayAdapter<String> adapter;
        private Post post;
        private SwipeRefreshLayout swipeLayout;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            // inflate view
            Context c = new ContextThemeWrapper(getActivity(),
                    R.style.Base_Theme_AppCompat_Light_DarkActionBar);
            inflater = inflater.cloneInContext(c);
            View view = inflater.inflate(R.layout.fragment_comments, container, false);
            view.findViewById(R.id.btn_send).setOnClickListener(this);

            // get post object
            post = getArguments().getParcelable(ARG_POST);
            if (post == null)
                throw new RuntimeException();

            // add comments to list view
            List<String> contents = new ArrayList<>();
            for (Comment comment : post.getComments())
                contents.add(comment.getContent());
            adapter = new ArrayAdapter<>(getContext(), R.layout.comment, R.id.content, contents);
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
            // get comment content
            EditText field = (EditText) view.findViewById(R.id.text_comment);
            String content = field.getText().toString().trim();
            Bundle args = getArguments();

            if (content.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.error_empty_reply),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // create comment
            Comment comment = post.comment(content, G::e);
            if (comment != null) {
                G.d(comment.getContent());
                adapter.add(comment.getContent());
            }

            // remove focus from reply box
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
            new Handler().post(() -> {
                List<Comment> newComments = post.refreshComments(G::e);
                for (Comment comment : newComments)
                    adapter.add(comment.getContent());
                swipeLayout.setRefreshing(false);
            });
        }
    }
}
