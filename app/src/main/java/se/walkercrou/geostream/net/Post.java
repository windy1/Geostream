package se.walkercrou.geostream.net;

import android.location.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Post that a user has created with a location and an image or video.
 */
public class Post {
    public static final String SERVER_URI = "/api/posts/";
    private final Location location;
    private final List<byte[]> data = new ArrayList<>();

    public Post(Location location, byte[] data) {
        this.location = location;
        this.data.add(data);
    }

    /**
     * Returns the location where this post was created.
     *
     * @return location post was created at
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Returns a list of frames for the media to be posted. If an image, the returned list will be
     * of length one.
     *
     * @return media data
     */
    public List<byte[]> getData() {
        return Collections.unmodifiableList(new ArrayList<>(data));
    }

    /**
     * Returns true if this post is a video determined by whether the data list is of length one.
     *
     * @return true if video
     */
    public boolean isVideo() {
        return data.size() > 1;
    }

    /**
     * Sends this post to the server.
     */
    public void sendInBackground() {
        Request request = new Request(Request.METHOD_POST, SERVER_URI);
        request.set("user", "http://10.245.77.244:8000/api/users/1/");
        request.set("lat", location.getLatitude());
        request.set("lng", location.getLongitude());
        request.set("media_file", new FileValue("post.bmp", data.get(0)));
        request.setAuthorization("walker", "quacking7");
        request.sendInBackground();
    }
}
