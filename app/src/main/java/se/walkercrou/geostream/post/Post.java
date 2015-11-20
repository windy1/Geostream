package se.walkercrou.geostream.post;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import se.walkercrou.geostream.net.ErrorCallback;
import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.request.ResourceCreateRequest;
import se.walkercrou.geostream.net.request.ResourceCreateRequest.FileValue;
import se.walkercrou.geostream.net.request.ResourceListRequest;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents a Post that a user has created with a location and an image or video.
 */
public class Post extends Resource implements Parcelable {
    // Post POST request parameters
    public static final String PARAM_ID = "id";
    public static final String PARAM_LAT = "lat";
    public static final String PARAM_LNG = "lng";
    public static final String PARAM_FILE = "media_file";
    public static final String PARAM_IS_VIDEO = "is_video";
    public static final String PARAM_CLIENT_SECRET = "client_secret";

    public static final String BASE_FILE_NAME = "media_file.bmp";

    private int id;
    private final Location location;
    private byte[] data;
    private String fileUrl;

    private Post(Location location, byte[] data) {
        this.location = location;
        this.data = data;
    }

    private Post(Location location, String fileUrl, int id) {
        this.location = location;
        this.fileUrl = fileUrl;
        this.id = id;
    }

    /**
     * Returns this Post's unique ID
     *
     * @return post id
     */
    public int getId() {
        return id;
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
    public byte[] getData() {
        return data;
    }

    /**
     * Returns true if this post is a video determined by whether the data list is of length one.
     *
     * @return true if video
     */
    public boolean isVideo() {
        return false;
    }

    /**
     * Returns the URL where this Post's media file resides.
     *
     * @return url of media file
     */
    public String getFileUrl() {
        return fileUrl;
    }

    /**
     * Sets the URL where this Post's media file resides.
     *
     * @param fileUrl url of media file
     */
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    /**
     * Starts this Post's detail activity from the specified context.
     *
     * @param c context to start from
     */
    public void startActivity(Context c) {
        Intent intent = new Intent(c, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST, this);
        c.startActivity(intent);
    }

    /**
     * Creates and returns a new Post object and sends a creation request to the server.
     *
     * @param location of post
     * @param data media data
     * @param callback error callback
     * @return new post object
     */
    public static Post create(Location location, String fileType, byte[] data, ErrorCallback callback) {
        // create post object
        Post post = new Post(location, data);

        // post to server
        ResourceResponse response = null;
        try {
            ResourceCreateRequest request = new ResourceCreateRequest(Resource.POSTS);
            request.set(PARAM_LAT, location.getLatitude())
                .set(PARAM_LNG, location.getLongitude())
                .set(PARAM_FILE, new FileValue(BASE_FILE_NAME, fileType, data))
                .set(PARAM_IS_VIDEO, post.isVideo());
            response = request.sendInBackground();
        } catch (MalformedURLException e) {
            // should never happen
            e.printStackTrace();
        }

        G.d(response);

        // check server response
        if (response == null) {
            callback.onError(null);
            return null;
        } else if (response.isError()) {
            callback.onError(response.getErrorDetail());
            return null;
        }

        // read response
        try {
            JSONObject body = (JSONObject) response.get();
            post.id = body.getInt(PARAM_ID); // set newly created id
            String secret = body.getString(PARAM_CLIENT_SECRET); // get client secret
            G.app.secrets.edit().putString(Integer.toString(post.id), secret).commit();
            post.setFileUrl(body.getString(PARAM_FILE));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return post;
    }

    /**
     * Returns a list of all Posts on the server.
     *
     * @param callback error callback
     * @return list of all posts
     */
    public static List<Post> all(ErrorCallback callback) {
        // send request to server
        ResourceResponse response = null;
        try {
            response = new ResourceListRequest(Resource.POSTS).sendInBackground();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        G.d(response);

        // read response
        if (response == null) {
            callback.onError(null);
            return null;
        } else if (response.isError()) {
            callback.onError(response.getErrorDetail());
            return null;
        }

        return parse((JSONArray) response.get());
    }

    private static List<Post> parse(JSONArray array) {
        List<Post> posts = new ArrayList<>(array.length());
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonPost = array.getJSONObject(i);
                Location loc = new Location(G.app.name);
                loc.setLatitude(jsonPost.getDouble(PARAM_LAT));
                loc.setLongitude(jsonPost.getDouble(PARAM_LNG));
                posts.add(new Post(loc, jsonPost.getString(PARAM_FILE), jsonPost.getInt(PARAM_ID)));
            }
        } catch (JSONException e) {
            G.e("An error occurred while parsing post data", e);
        }
        return posts;
    }

    // Parcelable implementation for passing posts between activities

    public static final Parcelable.Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel source) {
            Location location = source.readParcelable(null);
            String fileUrl = source.readString();
            int id = source.readInt();
            return new Post(location, fileUrl, id);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(location, flags);
        dest.writeString(fileUrl);
        dest.writeInt(id);
    }
}
