package se.walkercrou.geostream;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;

import se.walkercrou.geostream.net.Post;

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

        // enable up navigation
        ActionBar ab = getActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);

        // get the passed post object
        post = getIntent().getParcelableExtra(EXTRA_POST);
    }
}
