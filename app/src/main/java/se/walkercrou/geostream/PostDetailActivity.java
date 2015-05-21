package se.walkercrou.geostream;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

/**
 * Represents an activity that displays a post's details.
 */
public class PostDetailActivity extends Activity {
    public static final String EXTRA_POST = "post";
    private Post post;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_post_detail);

        setupActionBar();

        // get the passed post object
        post = getIntent().getParcelableExtra(EXTRA_POST);
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        if (bar == null)
            return;

        // enable up navigation
        bar.setDisplayHomeAsUpEnabled(true);
    }
}
