package se.walkercrou.geostream.post;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

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
        if (item.getItemId() == R.id.action_discard)
            discard();
        return super.onOptionsItemSelected(item);
    }

    private Bitmap downloadMedia() {
        MediaResponse response = new MediaRequest(post.getFileUrl()).sendInBackground();
        if (response == null)
            throw new RuntimeException("Could not download post media");
        else if (response.isError())
            throw new RuntimeException("Could not download post media: "
                    + response.getStatusCode());
        return response.get();
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
                    return new CommentsFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

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

    public static class CommentsFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            Context c = new ContextThemeWrapper(getActivity(),
                    R.style.Base_Theme_AppCompat_Light_DarkActionBar);
            inflater = inflater.cloneInContext(c);
            return inflater.inflate(R.layout.fragment_comments, container, false);
        }
    }
}
