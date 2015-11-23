package se.walkercrou.geostream.post;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import java.net.MalformedURLException;

import se.walkercrou.geostream.MapActivity;
import se.walkercrou.geostream.R;
import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.request.ResourceDeleteRequest;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents an activity that displays a post's details.
 */
public class PostDetailActivity extends Activity {
    public static final String EXTRA_POST = "post";
    private Post post;
    private String clientSecret;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_post_detail);

        setupActionBar();

        // get the passed post object
        post = getIntent().getParcelableExtra(EXTRA_POST);
        clientSecret = G.app.secrets.getString(Integer.toString(post.getId()), null);

        loadMedia();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_detail_activity_actions, menu);
        if (clientSecret != null)
            menu.getItem(0).setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_discard)
            discard();
        return super.onOptionsItemSelected(item);
    }

    private void loadMedia() {
        // TODO
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        if (bar == null)
            return;

        // enable up navigation
        bar.setDisplayHomeAsUpEnabled(true);
    }

    private void discard() {
        G.d("client secret = " + clientSecret);
        ResourceResponse response = null;
        try {
            response = new ResourceDeleteRequest(Resource.POSTS, post.getId(), clientSecret)
                .sendInBackground();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (response == null) {
            throw new RuntimeException("Could not discard post");
        } else if (response.isError()) {
            throw new RuntimeException("Could not discard post: " + response.getErrorDetail());
        }

        startActivity(new Intent(this, MapActivity.class));
    }
}
