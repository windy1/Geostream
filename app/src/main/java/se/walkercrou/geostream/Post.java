package se.walkercrou.geostream;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.walkercrou.geostream.net.request.ApiRequest;
import se.walkercrou.geostream.net.request.FileValue;
import se.walkercrou.geostream.net.request.Request;
import se.walkercrou.geostream.net.response.ApiResponse;
import se.walkercrou.geostream.util.AppUtil;
import se.walkercrou.geostream.util.ErrorCallback;

/**
 * Represents a Post that a user has created with a location and an image or video.
 */
public class Post implements Parcelable {
    // Post POST request parameters
    public static final String PARAM_LAT = "lat";
    public static final String PARAM_LNG = "lng";
    public static final String PARAM_FILE = "media_file";
    public static final String PARAM_IS_VIDEO = "is_video";

    public static final String BASE_FILE_NAME = "media_file.bmp";

    // posts that have been placed on the map
    private static final Map<Marker, Post> mappedPosts = new HashMap<>();

    private final Location location;
    private byte[] data;
    private String fileUrl;

    private Post(Location location, byte[] data) {
        this.location = location;
        this.data = data;
    }

    private Post(Location location, String fileUrl) {
        this.location = location;
        this.fileUrl = fileUrl;
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
     * Adds this Post to the specified {@link GoogleMap}.
     *
     * @param map to place post on
     */
    public void placeOnMap(GoogleMap map) {
        Marker marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude())));
        mappedPosts.put(marker, this);
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
    public static Post create(Location location, byte[] data, ErrorCallback callback) {
        Post post = new Post(location, data);
        // create on server
        ApiResponse response = new ApiRequest(Request.METHOD_POST, ApiRequest.URL_POST_LIST)
                .set(PARAM_LAT, location.getLatitude())
                .set(PARAM_LNG, location.getLongitude())
                .set(PARAM_FILE, new FileValue(BASE_FILE_NAME, data))
                .set(PARAM_IS_VIDEO, post.isVideo())
                .sendInBackground();

        // check if successful
        if (response == null) {
            callback.onError(null);
            return null;
        } else if (response.isError()) {
            callback.onError(response.getErrorDetail());
            return null;
        }

        try {
            // update the file location of the post
            post.setFileUrl(((JSONObject) response.get()).getString(Post.PARAM_FILE));
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
        // send request
        ApiResponse response = new ApiRequest(Request.METHOD_GET, ApiRequest.URL_POST_LIST)
                .sendInBackground();

        // check response
        if (response == null)
            // no connection
            callback.onError(null);
        else if (response.isError())
            callback.onError(response.getErrorDetail());

        // return results (if any)
        return response != null ? parse((JSONArray) response.get()) : null;
    }

    /**
     * Returns the Post linked to the specified map {@link Marker}.
     *
     * @param marker to get post for
     * @return post mapped to marker
     */
    public static Post getPostFor(Marker marker) {
        return mappedPosts.get(marker);
    }

    private static List<Post> parse(JSONArray array) {
        List<Post> posts = new ArrayList<>(array.length());
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonPost = array.getJSONObject(i);
                Location loc = new Location(AppUtil.getName());
                loc.setLatitude(jsonPost.getDouble(PARAM_LAT));
                loc.setLongitude(jsonPost.getDouble(PARAM_LNG));
                posts.add(new Post(loc, jsonPost.getString(PARAM_FILE)));
            }
        } catch (JSONException e) {
            AppUtil.e("An error occurred while parsing post data", e);
        }
        return posts;
    }

    // Parcelable implementation for passing posts between activities

    public static final Parcelable.Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel source) {
            Location location = source.readParcelable(null);
            String fileUrl = source.readString();
            return new Post(location, fileUrl);
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
    }
}
