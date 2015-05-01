package se.walkercrou.geostream;

import android.app.Activity;
import android.os.Bundle;

import se.walkercrou.geostream.App;
import se.walkercrou.geostream.net.Post;

public class PostDetailActivity extends Activity {
    public static final String EXTRA_POST = "post";

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        Post post = getIntent().getParcelableExtra("post");
        App.d("url = " + post.getFileUrl());
    }
}
